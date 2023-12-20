package de.konqi.roborockbridge

import de.konqi.roborockbridge.bridge.DeviceForPublish
import de.konqi.roborockbridge.bridge.MapDataForPublish
import de.konqi.roborockbridge.persistence.*
import de.konqi.roborockbridge.protocol.RoborockCredentials
import de.konqi.roborockbridge.protocol.RoborockMqtt
import de.konqi.roborockbridge.protocol.mqtt.StatusUpdate
import de.konqi.roborockbridge.bridge.S8UltraInterpreter
import de.konqi.roborockbridge.protocol.mqtt.MessageWrapper
import de.konqi.roborockbridge.protocol.mqtt.RequestMethod
import de.konqi.roborockbridge.protocol.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.protocol.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.protocol.mqtt.response.Protocol301
import de.konqi.roborockbridge.protocol.mqtt.response.MapDataWrapper
import de.konqi.roborockbridge.protocol.rest.HomeApi
import de.konqi.roborockbridge.protocol.rest.LoginApi
import de.konqi.roborockbridge.protocol.rest.UserApi
import de.konqi.roborockbridge.utility.cast
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service


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

        val robots = dataAccessLayer.saveDevices(homeDetails, homeEntity)

        // announce devices on mqtt broker
        bridgeMqtt.announceHome(homeEntity)
        robots.map {
            DeviceForPublish.fromDeviceEntity(
                it, /* TODO: Select correct interpreter */
                S8UltraInterpreter()
            )
        }.forEach(bridgeMqtt::announceDevice)
        bridgeMqtt.announceRooms(rooms.toList())

        val schemasFromRoborock = userApi.getCleanupSchemas(homeEntity.homeId)
        val schemas =
            dataAccessLayer.saveSchemas(schemasFromRoborock, homeEntity)
        bridgeMqtt.announceSchemas(schemas.toList())

        // logger.info("roborock://mqtt-eu-3.roborock.com:8883?u=${roborockCredentials.userId}&s=${roborockCredentials.sessionId}&k=${roborockCredentials.mqttKey}&did=${robots.first().deviceId}&key=${robots.first().deviceKey}&pin=1234")
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

    private fun publishDeviceStatus(deviceId: String) {
        val device = dataAccessLayer.getDevice(deviceId)
        device.ifPresent {
            // TODO select correct interpreter
            val interpreter = S8UltraInterpreter()
            bridgeMqtt.announceDevice(DeviceForPublish.fromDeviceEntity(it, interpreter))
        }
    }

    private fun roborockMqttProcessingLoop() {
        while (roborockMqtt.inboundMessagesQueue.size > 0) {
            val message = roborockMqtt.inboundMessagesQueue.remove()
            when (message.messageSchemaType) {
                IpcRequestWrapper.SCHEMA_TYPE -> { /* We should not receive requests */}
                IpcResponseWrapper.SCHEMA_TYPE -> {}
                MapDataWrapper.SCHEMA_TYPE -> {
                    val payload = cast<MessageWrapper<Protocol301>>(message).payload

                    val homeId = dataAccessLayer.getDevice(message.deviceId).get().home.homeId
                    val mapData = MapDataForPublish(map = payload.payload.map?.getImageDataUrl(),
                        robotPosition = payload.payload.robotPosition,
                        chargerPosition = payload.payload.chargerPosition,
                        gotoPath = payload.payload.gotoPath?.points,
                        path = payload.payload.path?.points,
                        predictedPath = payload.payload.predictedPath?.points)
                    bridgeMqtt.publishMapData(homeId = homeId, deviceId = message.deviceId, mapData)
                }

                else -> {
                    message as StatusUpdate
                    val property = message.messageSchemaType
                    val value = message.value
                    dataAccessLayer.updateDeviceState(message.deviceId, property, value)
                    logger.debug("Status of '$property' is now '$value' for device ${message.deviceId}.")

                    // notify
                    publishDeviceStatus(message.deviceId)
                }
            }
        }
    }

    private fun bridgeMqttProcessingLoop() {
        while (bridgeMqtt.inboundMessagesQueue.size > 0) {
            when (val command = bridgeMqtt.inboundMessagesQueue.remove()) {
                is ActionCommand -> {
                    if (command.action == ActionEnum.HOME &&
                        command.target.type == TargetType.DEVICE &&
                        !command.target.identifier.isNullOrBlank()
                    ) {
                        logger.info("Requesting device '${command.target.identifier}' to return to dock via mqtt.")
                        roborockMqtt.publishRequest(command.target.identifier, RequestMethod.APP_CHARGE)
                    } else if (command.action == ActionEnum.START_SCHEMA &&
                        command.target.type == TargetType.DEVICE &&
                        !command.target.identifier.isNullOrBlank()
                    ) {
                        val schemaId = command.arguments["schemaId"]?.toInt()
                        if (schemaId != null) {
                            logger.info("Requesting '$schemaId' schema cleanup via rest api.")
                            userApi.startCleanupSchema(schemaId)
                        } else {
                            logger.warn("Command is missing argument 'schemaId'.")
                        }
                    } else {
                        logger.warn("ActionCommand (targetType=${command.target.type},action=${command.action.value}) type not implemented")
                    }
                }

                is GetCommand -> {
                    if (command.target.type == TargetType.DEVICE && !command.target.identifier.isNullOrEmpty()) {
                        if (command.parameters.first() == "status") {
                            logger.info("Requesting device state refresh via mqtt.")
                            roborockMqtt.publishStatusRequest(command.target.identifier)
                        } else if (command.parameters.first() == "map") {
                            logger.info("Requesting device map via mqtt.")
                            roborockMqtt.publishMapRequest(command.target.identifier)
                        }
                    } else if (command.target.type == TargetType.HOME && !command.target.identifier.isNullOrEmpty()) {
                        logger.info("Refreshing home details via rest api.")
                        init()
                    } else {
                        logger.warn(
                            "GetCommand type (targetType=${command.target.type},parameters=[${
                                command.parameters.joinToString(
                                    ","
                                )
                            }]) not implemented"
                        )
                    }
                }

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