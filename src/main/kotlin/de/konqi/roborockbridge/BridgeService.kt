package de.konqi.roborockbridge

import de.konqi.roborockbridge.bridge.*
import de.konqi.roborockbridge.bridge.dto.*
import de.konqi.roborockbridge.bridge.interpreter.BridgeDeviceState
import de.konqi.roborockbridge.bridge.interpreter.InterpreterProvider
import de.konqi.roborockbridge.persistence.DataAccessLayer
import de.konqi.roborockbridge.remote.RoborockCredentials
import de.konqi.roborockbridge.remote.helper.RequestMemory
import de.konqi.roborockbridge.remote.mqtt.MessageWrapper
import de.konqi.roborockbridge.remote.mqtt.RequestMethod
import de.konqi.roborockbridge.remote.mqtt.RoborockMqtt
import de.konqi.roborockbridge.remote.mqtt.StatusUpdate
import de.konqi.roborockbridge.remote.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppSegmentCleanRequestDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppStartDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.SetCleanMotorModeDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.StringDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.remote.mqtt.ipc.response.payload.GetPropGetStatusResponse
import de.konqi.roborockbridge.remote.mqtt.ipc.response.payload.RoomMapping
import de.konqi.roborockbridge.remote.mqtt.map.MapDataWrapper
import de.konqi.roborockbridge.remote.mqtt.map.Protocol301
import de.konqi.roborockbridge.remote.rest.HomeApi
import de.konqi.roborockbridge.remote.rest.LoginApi
import de.konqi.roborockbridge.remote.rest.UserApi
import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.utility.cast
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
@Profile("bridge")
class BridgeService(
    @Autowired private val roborockMqtt: RoborockMqtt,
    @Autowired private val loginApi: LoginApi,
    @Autowired private val homeApi: HomeApi,
    @Autowired private val userApi: UserApi,
    @Autowired private val roborockCredentials: RoborockCredentials,
    @Autowired private val bridgeMqtt: BridgeMqtt,
    @Autowired private val dataAccessLayer: DataAccessLayer,
    @Autowired private val interpreterProvider: InterpreterProvider,
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val bridgeDeviceStateManager: BridgeDeviceStateManager
) {
    private var run = true

    fun init() {
        if (!roborockCredentials.isLoggedIn) {
            val loginData = loginApi.login()
            roborockCredentials.fromRriot(loginData.rriot, loginData.token)
        }

        val home = homeApi.getHome()
        val homeEntity = dataAccessLayer.saveHome(home)

        val homeDetails = userApi.getUserHome(homeEntity.homeId)

        val rooms = dataAccessLayer.saveRooms(homeDetails, homeEntity)

        val devices = dataAccessLayer.saveDevices(homeDetails, homeEntity)
        bridgeDeviceStateManager.updateDeviceState(devices)

        // announce devices on mqtt broker
        bridgeMqtt.announceHome(HomeForPublish.fromHomeEntity(homeEntity))
        devices.map(DeviceForPublish::fromDeviceEntity).forEach { device ->
            bridgeMqtt.announceDevice(device)
            publishDeviceStatus(deviceId = device.deviceId)
        }

        bridgeMqtt.announceRooms(rooms.map(RoomForPublish::fromRoomEntity))

        val schemasFromRoborock = userApi.getCleanupScenes(homeEntity.homeId)
        val schemas =
            dataAccessLayer.saveRoutines(schemasFromRoborock, homeEntity)

        bridgeMqtt.announceRoutines(schemas.map(SchemaForPublish::fromSchemaEntity))
    }

    @Scheduled(fixedDelay = 10_000)
    fun mqttStatusPoll() {
        // Clear stale messages and set associated devices to state unreachable
        requestMemory.clearMessagesOlderThan(5000).map { it.first }.toSet().forEach { deviceId ->
            bridgeDeviceStateManager.setDeviceState(deviceId, BridgeDeviceState.UNREACHABLE)
        }

        // poll active devices
        bridgeDeviceStateManager.getDevicesInState(BridgeDeviceState.ACTIVE, BridgeDeviceState.ERROR_ACTIVE)
            .forEach { deviceId ->
                roborockMqtt.publishStatusRequest(deviceId)
                roborockMqtt.publishMapRequest(deviceId)
            }
    }

    @Scheduled(fixedDelay = 5 * 90_000)
    fun restStatusPoll() {
        dataAccessLayer.getHomes().forEach { home ->
            val devices = dataAccessLayer.saveDevices(userApi.getUserHome(home.homeId), home)
            bridgeDeviceStateManager.updateDeviceState(devices)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun worker() {
        while (!bridgeMqtt.mqttClient.isConnected) {
            Thread.sleep(1000)
        }
        init()
        roborockMqtt.start()

        while (run) {
            bridgeMqttProcessingLoop()
            roborockMqttProcessingLoop()

            Thread.sleep(200)
        }
    }

    private fun publishDeviceStatus(deviceId: String, onlyUpdatesAfter: Date? = null) {
        val device = dataAccessLayer.getDevice(deviceId).get()
        val interpreter = interpreterProvider.getInterpreterForDevice(device)
        if (interpreter != null) {
            val statesToPublish = dataAccessLayer.getAllDeviceStatesModifiedAfterDate(deviceId, onlyUpdatesAfter)
            statesToPublish.forEach {
                bridgeMqtt.publishDeviceState(
                    homeId = device.home.homeId,
                    deviceId = device.deviceId,
                    deviceState = DeviceStateForPublish.fromDeviceStateEntity(it, interpreter)
                )
            }
        } else {
            logger.error("No interpreter available for device model '${device.model}'")
        }
    }

    private fun roborockMqttProcessingLoop() {
        while (roborockMqtt.inboundMessagesQueue.size > 0) {
            val message = roborockMqtt.inboundMessagesQueue.remove()
            when (message.messageSchemaType) {
                IpcRequestWrapper.SCHEMA_TYPE -> { /* We should not receive requests */
                }

                IpcResponseWrapper.SCHEMA_TYPE -> {
                    val payload = cast<MessageWrapper<IpcResponseDps<*>>>(message).payload
                    if (payload.result != null) {
                        if (payload.method == RequestMethod.GET_PROP) {
                            val notifyAboutChangesAfter = Date()
                            val result = cast<Array<GetPropGetStatusResponse>>(payload.result).first()
                            dataAccessLayer.updateDeviceStates(message.deviceId, result.states)
                            bridgeDeviceStateManager.updateDeviceState(message.deviceId, result.states)

                            // notify
                            publishDeviceStatus(message.deviceId, notifyAboutChangesAfter)
                        } else if (payload.method == RequestMethod.GET_ROOM_MAPPING) {
                            val result = cast<Array<RoomMapping>>(payload.result)
                            val homeId = dataAccessLayer.getDevice(message.deviceId).get().home.homeId
                            val rooms = dataAccessLayer.getRoomsForHome(homeId = homeId)

                            val listOfRestRoomIds = result.map { it.restRoomId.toInt() }
                            val updatedRooms = rooms.filter { it.roomId in listOfRestRoomIds }
                                .map { roomFromDb ->
                                    roomFromDb.copy(mqttRoomId = result.find { it.restRoomId.toInt() == roomFromDb.roomId }?.mqttRoomId)
                                }.also { dataAccessLayer.saveRooms(it) }

                            // NOTIFY
                            bridgeMqtt.announceRooms(updatedRooms.map(RoomForPublish::fromRoomEntity))
                        }
                    }
                }

                MapDataWrapper.SCHEMA_TYPE -> {
                    val mapDataPayload = cast<MessageWrapper<Protocol301>>(message).payload.payload

                    val homeId = dataAccessLayer.getDevice(message.deviceId).get().home.homeId
                    val mapData = MapDataForPublish.fromProtocol301Payload(mapDataPayload)
                    bridgeMqtt.publishMapData(homeId = homeId, deviceId = message.deviceId, mapData)
                }

                else -> {
                    message as StatusUpdate

                    val schemaId = message.messageSchemaType
                    val value = message.value
                    val code = interpreterProvider
                        .getInterpreterForDevice(message.deviceId)?.schemaIdToPropName(schemaId)

                    if (code != null) {
                        val notifyAboutChangesAfter = Date()
                        dataAccessLayer.updateDeviceState(message.deviceId, code, value)
                        logger.debug("Status of '$code' is now '$value' for device ${message.deviceId}.")

                        // notify
                        publishDeviceStatus(message.deviceId, notifyAboutChangesAfter)
                    } else {
                        logger.warn("Update for value with schemaId '$schemaId' has no corresponding code in interpreter and will be ignored.")
                    }
                }
            }
        }
    }

    private fun bridgeMqttProcessingLoop() {
        while (bridgeMqtt.inboundMessagesQueue.size > 0) {
            val incomingMessage = bridgeMqtt.inboundMessagesQueue.remove()
            if (incomingMessage.header.targetIdentifier == null) {
                logger.warn("target identifier of message is empty")
                continue
            }
            val targetType = incomingMessage.header.targetType
            val targetIdentifier = incomingMessage.header.targetIdentifier
            val actionKeyword = incomingMessage.body.actionKeyword

            when (incomingMessage.header.command) {
                CommandType.ACTION -> {
                    when (targetType) {
                        TargetType.DEVICE -> {
                            when (actionKeyword) {
                                ActionKeywordsEnum.HOME -> {
                                    logger.info("Requesting device '${targetIdentifier}' to return to dock via mqtt.")
                                    roborockMqtt.publishRequest<Unit>(
                                        targetIdentifier,
                                        RequestMethod.APP_CHARGE
                                    )
                                }

                                ActionKeywordsEnum.SEGMENTS -> {
                                    val params = incomingMessage.body.parameters as AppSegmentCleanRequestDTO
                                    logger.info(
                                        "Requesting device '${targetIdentifier}' to clean segments ${
                                            params.segments.joinToString(
                                                ", "
                                            )
                                        }."
                                    )
                                    roborockMqtt.publishCleanSegmentRequest(targetIdentifier, params)
                                }

                                ActionKeywordsEnum.START -> {
                                    val params = incomingMessage.body.parameters as AppStartDTO
                                    logger.info("Starting / Resuming device '$targetIdentifier'")
                                    roborockMqtt.publishStartRequest(targetIdentifier, params)
                                }

                                ActionKeywordsEnum.PAUSE -> {
                                    logger.info("Pausing device '$targetIdentifier'")
                                    roborockMqtt.publishPauseRequest(targetIdentifier)
                                }

                                ActionKeywordsEnum.CLEAN_MODE -> {
                                    val params = incomingMessage.body.parameters as SetCleanMotorModeDTO
                                    logger.info("Setting cleanup mode for '$targetIdentifier'.")
                                    roborockMqtt.publishSetCleanMotorMode(targetIdentifier, params)
                                }

                                else -> {
                                    logger.warn("currently only 'home' is a valid argument for device action.")
                                }
                            }
                        }

                        TargetType.ROUTINE -> {
                            val routineId = targetIdentifier.toInt()
                            logger.info("Requesting cleanup routine '${targetIdentifier}' via rest api.")
                            userApi.startCleanupSchema(routineId)

                            // Assume affected devices become active
                            dataAccessLayer.getRoutine(routineId).ifPresent {
                                it.triggeredDeviceIds.forEach { deviceId ->
                                    bridgeDeviceStateManager.setDeviceState(
                                        deviceId = deviceId,
                                        BridgeDeviceState.ACTIVE
                                    )
                                }
                            }
                        }

                        else -> {
                            logger.warn("ActionCommand (targetType=${targetType}, actionKeyword=${actionKeyword}) type not implemented")
                        }
                    }
                }

                CommandType.GET -> {
                    if (targetType == TargetType.DEVICE) {
                        if (actionKeyword == ActionKeywordsEnum.STATE) {
                            logger.info("Requesting device state refresh via mqtt.")
                            roborockMqtt.publishStatusRequest(targetIdentifier)
                            roborockMqtt.publishRoomMappingRequest(targetIdentifier)
                        } else if (actionKeyword == ActionKeywordsEnum.MAP) {
                            logger.info("Requesting device map via mqtt.")
                            roborockMqtt.publishMapRequest(targetIdentifier)
                        }
                    } else if (targetType == TargetType.HOME) {
                        logger.info("Refreshing home details via rest api.")
                        init()
                    } else {
                        logger.warn(
                            "GetCommand type (targetType=${targetType}) not implemented"
                        )
                    }
                }

                CommandType.SET -> {
                    if (targetType == TargetType.DEVICE_PROPERTY) {
                        val value = (incomingMessage.body.parameters as? StringDTO)?.value
                        if (targetIdentifier == "fan_power" && value != null) {
                            val intValue = value.toInt()
                            logger.info("Setting fan_power to $intValue")
                            // this may not be correct for all robots, it is for S8 Pro Ultra
                            roborockMqtt.publishSetCustomMode(incomingMessage.header.deviceId!!, intValue)
                        } else {
                            logger.warn("Cannot set $targetIdentifier property at the moment.")
                        }
                    } else {
                        logger.warn("Cannot set anything, but device properties at the moment.")
                    }
                }

                else -> {
                    logger.warn("Command $incomingMessage not implemented")
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down bridge service")
        run = false
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}