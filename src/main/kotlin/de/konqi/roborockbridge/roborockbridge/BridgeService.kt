package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.persistence.HomeRepository
import de.konqi.roborockbridge.roborockbridge.persistence.RobotRepository
import de.konqi.roborockbridge.roborockbridge.persistence.RoomRepository
import de.konqi.roborockbridge.roborockbridge.persistence.SchemaRepository
import de.konqi.roborockbridge.roborockbridge.persistence.entity.*
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockCredentials
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockMqtt
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.GetPropGetStatusResponse
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.roborockbridge.protocol.rest.HomeApi
import de.konqi.roborockbridge.roborockbridge.protocol.rest.LoginApi
import de.konqi.roborockbridge.roborockbridge.protocol.rest.UserApi
import de.konqi.roborockbridge.roborockbridge.utility.cast
import de.konqi.roborockbridge.roborockbridge.utility.checkType
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue

enum class QueueMessageType {
    A, B;
}

data class QueueMessage<T>(
    val type: QueueMessageType,

    val content: T
)

class InterThreadCommunication {
    val queue = ConcurrentLinkedQueue<QueueMessage<*>>()

}

@Service
//@Profile("off")
class BridgeService(
    @Autowired private val roborockMqtt: RoborockMqtt,
    @Autowired private val loginApi: LoginApi,
    @Autowired private val homeApi: HomeApi,
    @Autowired private val userApi: UserApi,
    @Autowired private val roborockCredentials: RoborockCredentials,
    @Autowired private val bridgeMqtt: BridgeMqtt,
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository,
    private val robotRepository: RobotRepository,
    private val schemaRepository: SchemaRepository
) {

    private var run = true

    fun init() {
        if (!roborockCredentials.isLoggedIn) {
            val loginData = loginApi.login()
            roborockCredentials.fromRriot(loginData.rriot, loginData.token)
        }

        val home = homeApi.getHome()
        val homeEntity = homeRepository.save(Home(homeId = home.rrHomeId, name = home.name))

        val homeDetails = userApi.getUserHome(homeEntity.homeId)

        val rooms = roomRepository.saveAll(
            homeDetails.rooms.map { Room(home = homeEntity, roomId = it.id, name = it.name) }
        )

        val robots = homeDetails.devices.map { device ->
            val product = homeDetails.products.find { product -> product.id == device.productId }
                ?: throw RuntimeException("Unable to resolve product information for product id '${device.productId}'")

            val status: List<RobotState> =
                device.deviceStatus.map { status ->
                    val protocolInfo = product.schema.find { it.id.toInt() == status.key }
                        ?: throw RuntimeException("Unable to resolve state meta data")

                    RobotState(
                        protocolId = status.key,
                        code = protocolInfo.code,
                        value = status.value,
                        mode = ProtocolMode.valueOf(protocolInfo.mode.uppercase()),
                        property = protocolInfo.property,
                        type = protocolInfo.type
                    )
                }

            Robot(
                home = homeEntity,
                deviceId = device.duid,
                name = device.name,
                deviceKey = device.localKey,
                productName = product.name,
                model = product.model,
                firmwareVersion = device.fv,
                serialNumber = device.sn,
                state = status
            )
        }.run { robotRepository.saveAll(this) }

        // announce devices on mqtt broker
        bridgeMqtt.announceHome(homeEntity)
        robots.forEach(bridgeMqtt::announceDevice)
        bridgeMqtt.announceRooms(rooms.toList())

        val schemasFromRoborock = userApi.getCleanupSchemas(homeEntity.homeId)
        val schemas =
            schemasFromRoborock.map { schema -> Schema(home = homeEntity, schemaId = schema.id, name = schema.name) }
                .run {
                    schemaRepository.saveAll(this)
                }
        bridgeMqtt.announceSchemas(schemas.toList())
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
//        mqttClient.connect()
//
//        val deviceId = roborockData.robots[0].deviceId
//        val deviceLocalKey = roborockData.robots.first { it.deviceId == deviceId }.deviceInformation.localKey
//        mqttClient.monitorDevice(deviceId = deviceId, key = deviceLocalKey)

//        while (run) {
//        println("sleep 1000ms")
//        Thread.sleep(1000)
//        mqttClient.publishStatusRequest(deviceId = deviceId, deviceLocalKey = deviceLocalKey)

//        println("sleep 5000ms (wait for response)")
//        Thread.sleep(5000)
//        println("sleep ended")
//        }
    }

    final inline fun <reified T> foo(value: Any): T = if (value is T) value else throw ClassCastException()

    private fun roborockMqttProcessingLoop() {
        while (roborockMqtt.inboundMessagesQueue.size > 0) {
            val message = roborockMqtt.inboundMessagesQueue.remove()
            logger.info("I don't know what to do with this message. $message")
            if (checkType<IpcResponseDps<GetPropGetStatusResponse>>(message)) {
                val bar = cast<IpcResponseDps<GetPropGetStatusResponse>>(message)
//                bridgeMqtt.publishVolatile(homeId = , deviceId = )
            }
        }
    }

    // {"dps": {"121":"6","128":"2"}} going home
    // {"dps": {"128":"10"}} positioning
    // {"dps": {"128":"2"}} positioning successful
    // {"dps": {"128":"0"}}, {"dps": {"128":"4"}}, {"dps": {"121":"8"}}, {"dps": {"128":"0"}} returned home, charging

    private fun bridgeMqttProcessingLoop() {
        while (bridgeMqtt.inboundMessagesQueue.size > 0) {
            when (val command = bridgeMqtt.inboundMessagesQueue.remove()) {
                is ActionCommand -> {
                    if (command.action == ActionEnum.HOME &&
                        command.target.type == TargetType.DEVICE &&
                        !command.target.identifier.isNullOrBlank()
                    ) {
                        logger.info("Sending device '${command.target.identifier}' home.")
                        roborockMqtt.publishReturnToChargingStation(command.target.identifier)
                    } else {
                        logger.warn("ActionCommand (targetType=${command.target.type},action=${command.action.value}) type not implemented")
                    }
                }

                is GetCommand -> {
                    if (command.target.type == TargetType.DEVICE && !command.target.identifier.isNullOrEmpty() && command.parameters.first() == "status") {
                        logger.info("Say please, or I won't GET stuff for ${command.target.type} ${command.target.identifier}")
                        roborockMqtt.publishStatusRequest(command.target.identifier)
                    } else if (command.target.type == TargetType.HOME && !command.target.identifier.isNullOrEmpty()) {
                        logger.info("Refreshing home details via rest api")
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