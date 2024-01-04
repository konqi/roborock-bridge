package de.konqi.roborockbridge

import de.konqi.roborockbridge.bridge.*
import de.konqi.roborockbridge.bridge.interpreter.BridgeDeviceState
import de.konqi.roborockbridge.bridge.interpreter.InterpreterProvider
import de.konqi.roborockbridge.bridge.interpreter.getState
import de.konqi.roborockbridge.persistence.*
import de.konqi.roborockbridge.persistence.entity.Device
import de.konqi.roborockbridge.remote.RoborockCredentials
import de.konqi.roborockbridge.remote.mqtt.RoborockMqtt
import de.konqi.roborockbridge.remote.mqtt.StatusUpdate
import de.konqi.roborockbridge.remote.mqtt.MessageWrapper
import de.konqi.roborockbridge.remote.mqtt.RequestMethod
import de.konqi.roborockbridge.remote.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.remote.mqtt.ipc.response.GetPropGetStatusResponse
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.remote.mqtt.ipc.response.RoomMapping
import de.konqi.roborockbridge.remote.mqtt.response.Protocol301
import de.konqi.roborockbridge.remote.mqtt.response.MapDataWrapper
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
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*

@Component
class BridgeDeviceStateManager(
    @Autowired private val interpreterProvider: InterpreterProvider
) {
    private val deviceStates: MutableMap<String, BridgeDeviceState> = mutableMapOf()

    fun updateDeviceState(device: Device) {
        deviceStates[device.deviceId] =
            interpreterProvider.getInterpreterForDevice(device)?.getState(device.state) ?: BridgeDeviceState.UNKNOWN
    }

    fun updateDeviceState(devices: List<Device>) {
        devices.forEach(this::updateDeviceState)
    }

    fun updateDeviceState(deviceId: String, states: Map<String, Int>) {
        deviceStates[deviceId] =
            interpreterProvider.getInterpreterForDevice(deviceId)?.getState(states) ?: BridgeDeviceState.UNKNOWN
    }

    fun setDeviceState(deviceId: String, state: BridgeDeviceState) {
        deviceStates[deviceId] = state
    }

    fun getDevicesInState(vararg state: BridgeDeviceState) =
        deviceStates.filter { device -> state.any { it == device.value } }.keys
}

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
        bridgeMqtt.announceHome(homeEntity)
        devices.map(DeviceForPublish::fromDeviceEntity).forEach { device ->
            bridgeMqtt.announceDevice(device)
            publishDeviceStatus(deviceId = device.deviceId)
        }

        bridgeMqtt.announceRooms(rooms.toList())

        val schemasFromRoborock = userApi.getCleanupScenes(homeEntity.homeId)
        val schemas =
            dataAccessLayer.saveRoutines(schemasFromRoborock, homeEntity)

        bridgeMqtt.announceRoutines(schemas.map(SchemaForPublish::fromSchemaEntity))
    }

    @Scheduled(fixedDelay = 10_000)
    fun mqttStatusPoll() {
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
                            bridgeMqtt.announceRooms(updatedRooms)
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
            when (val command = bridgeMqtt.inboundMessagesQueue.remove()) {
                is ActionCommand -> {
                    when (command.target.type) {
                        TargetType.DEVICE -> {
                            when (command.actionKeyword) {
                                ActionKeywordsEnum.HOME -> {
                                    logger.info("Requesting device '${command.target.identifier}' to return to dock via mqtt.")
                                    roborockMqtt.publishRequest<Unit>(
                                        command.target.identifier,
                                        RequestMethod.APP_CHARGE
                                    )
                                }

//                                TODO implement - think about action keywords and params
//                                ActionKeywordsEnum.SEGMENTS -> {
//                                    roborockMqtt.publishCleanSegmentRequest(command.target.identifier, listOf())
//                                }

                                else -> {
                                    logger.warn("currently only 'home' is a valid argument for device action.")
                                }
                            }
                        }

                        TargetType.ROUTINE -> {
                            logger.info("Requesting cleanup routine '${command.target.identifier}' via rest api.")
                            userApi.startCleanupSchema(command.target.identifier.toInt())
                        }

                        else -> {
                            logger.warn("ActionCommand (targetType=${command.target.type}, actionKeyword=${command.actionKeyword}) type not implemented")
                        }
                    }
                }

                is GetCommand -> {
                    if (command.target.type == TargetType.DEVICE && command.target.identifier.isNotEmpty()) {
                        if (command.actionKeyword == ActionKeywordsEnum.STATE) {
                            logger.info("Requesting device state refresh via mqtt.")
                            roborockMqtt.publishStatusRequest(command.target.identifier)
                            roborockMqtt.publishRoomMappingRequest(command.target.identifier)
                        } else if (command.actionKeyword == ActionKeywordsEnum.MAP) {
                            logger.info("Requesting device map via mqtt.")
                            roborockMqtt.publishMapRequest(command.target.identifier)
                        }
                    } else if (command.target.type == TargetType.HOME && command.target.identifier.isNotEmpty()) {
                        logger.info("Refreshing home details via rest api.")
                        init()
                    } else {
                        logger.warn(
                            "GetCommand type (targetType=${command.target.type}) not implemented"
                        )
                    }
                }

//              TODO implement - think about params and topic/target for set with multiple values
//                is SetCommand -> {
//                    if(command.target.type == TargetType.DEVICE && command.target.identifier.isNotEmpty()) {
//                        if(command.what == "set_clean_motor_mode") {
//                            roborockMqtt.publishSetCleanMotorMode()
//                        }
//                        else if(command.what == "set_custom_mode") {
//                            roborockMqtt.publishSetCustomMode()
//                        }
//                        else {
//                            logger.warn("SetCommand for unknown property")
//                        }
//                    }
//                }

                else -> {
                    logger.warn("Command $command not implemented")
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