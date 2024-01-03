package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.utility.CircularConcurrentLinkedQueue
import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.utility.camelToSnakeCase
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class Message(
    val topic: String,
    val message: ByteArray,
    val qos: Int = 0,
    val retained: Boolean = false
)

data class Target(val type: TargetType, val identifier: String)

abstract class Command(
    val target: Target
)

class ActionCommand(target: Target, val actionKeyword: ActionKeywordsEnum = ActionKeywordsEnum.UNKNOWN) :
    Command(target)

class GetCommand(target: Target, val actionKeyword: ActionKeywordsEnum = ActionKeywordsEnum.UNKNOWN) : Command(target)


@ConfigurationProperties(prefix = "bridge-mqtt")
data class BridgeMqttConfig(
    val url: String,
    val clientId: String,
    val baseTopic: String,
    val username: String?,
    val password: String?
)

@Component
@Profile("bridge")
@EnableConfigurationProperties(BridgeMqttConfig::class)
class BridgeMqtt(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig, @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val receivedMessageParser: ReceivedMessageParser
) {
    val mqttClient = MqttClient(bridgeMqttConfig.url, bridgeMqttConfig.clientId, null)
    val outboundMessagesQueue: Queue<Message> = ConcurrentLinkedQueue()
    val inboundMessagesQueue = CircularConcurrentLinkedQueue<Command>(20)

    @PostConstruct
    fun init() {
        connect()
    }

    fun connect() {
        mqttClient.run {
            setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    logger.warn("Connection lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    logger.debug("New message for topic '$topic'")
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {
                    logger.debug("Message delivered")
                }
            })

            try {
                connect(MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    isAutomaticReconnect = true

                    if(!bridgeMqttConfig.username.isNullOrBlank()) {
                        userName = bridgeMqttConfig.username
                    }
                    if(!bridgeMqttConfig.password.isNullOrBlank()) {
                        password = bridgeMqttConfig.password.toCharArray()
                    }
                })
            } catch (e: Exception) {
                logger.error("Could not connect to ${bridgeMqttConfig.url}. ${e}")
                throw e
            }

            val handler = IMqttMessageListener { topic, message ->
                try {
                    handleMessage(topic, message)
                } catch (e: Exception) {
                    logger.error("Error while processing message: ${e.message}, Stacktrace: ${e.printStackTrace()}")
                }
            }

            val topicsToSubscribe = arrayOf(
                "${bridgeMqttConfig.baseTopic}/$HOME/+/$ROUTINE/+/action",
                "${bridgeMqttConfig.baseTopic}/$HOME/+/$DEVICE/+/get",
                "${bridgeMqttConfig.baseTopic}/$HOME/+/$DEVICE/+/set",
                "${bridgeMqttConfig.baseTopic}/$HOME/+/$DEVICE/+/action",
                "${bridgeMqttConfig.baseTopic}/$HOME/+/$DEVICE/+/+/set"
            )

            subscribe(
                topicsToSubscribe,
                IntArray(topicsToSubscribe.size) { 0 },
                Array(topicsToSubscribe.size) { handler })
        }
    }

    fun handleMessage(topic: String, message: MqttMessage) {
        val msg = receivedMessageParser.parse(topic, message.payload)
        when (msg.command) {
            CommandType.UNKNOWN -> {
                logger.debug("Topic '$topic' is not a command, ignoring it.")
            }

            CommandType.ACTION -> {
                if (msg.targetIdentifier == null) {
                    logger.warn("Unable to determine command recipient.")
                } else {
                    logger.info("Received action command for ${msg.targetType} '${msg.targetIdentifier}'.")

                    inboundMessagesQueue.add(
                        ActionCommand(
                            target = Target(
                                type = msg.targetType,
                                identifier = msg.targetIdentifier
                            ),
                            actionKeyword = msg.actionKeyword
                        )
                    )
                }
            }

            CommandType.GET -> {
                if (msg.targetIdentifier == null) {
                    logger.warn("Unable to determine command recipient.")
                } else {
                    inboundMessagesQueue.add(
                        GetCommand(
                            target = Target(type = msg.targetType, identifier = msg.targetIdentifier),
                            actionKeyword = msg.actionKeyword
                        )
                    )
                }
            }

            else -> {
                log("Command '${msg.command}' not implemented.")
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun queueWorker() {
        while (!outboundMessagesQueue.isEmpty()) {
            val message = outboundMessagesQueue.remove()
            if (mqttClient.isConnected) {
                mqttClient.publish(message.topic, message.message, message.qos, message.retained)
            } else {
                logger.warn("Could not publish message because mqtt client is not connected.")
            }
        }
    }

    fun log(message: String, homeId: Int? = null, deviceId: String? = null) {
        logger.info("Logging message '$message' back to broker.")
        val topic = if (deviceId != null && homeId != null) {
            getDeviceLogTopic(homeId, deviceId)
        } else {
            getBridgeLogTopic()
        }

        outboundMessagesQueue.add(Message(topic, message.toByteArray(), 0, false))
    }

    fun announceHome(home: Home) {
        logger.info("Announcing new home with id '${home.homeId}'")
        val topic = getHomeTopic(home.homeId)
        val payload = objectMapper.writeValueAsBytes(home)
        mqttClient.publish(topic, payload, 0, true)
    }

    fun announceDevice(device: DeviceForPublish) {
        logger.info("Announcing new robot with id '${device.deviceId}'")
        val topic = getDeviceTopic(homeId = device.homeId, deviceId = device.deviceId)
        val payload = objectMapper.writeValueAsBytes(device)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun publishDeviceState(homeId: Int, deviceId: String, deviceState: DeviceStateForPublish) {
        val topic = getPropertyTopic(homeId = homeId, deviceId = deviceId, deviceState.name)
        val payload = objectMapper.writeValueAsBytes(deviceState)
        mqttClient.publish(topic, payload, 0, true)
    }

    fun announceRooms(rooms: List<Room>) {
        logger.info("Announcing ${rooms.size} rooms.")
        val topic = ROOM_TOPIC.replace(HOME_TOPIC, getHomeTopic(rooms.first().home.homeId))
        val payload = objectMapper.writeValueAsBytes(rooms)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun announceRoutines(schemas: List<SchemaForPublish>) {
        logger.info("Announcing ${schemas.size} schemas.")
        schemas.forEach {
            val topic = getRoutineTopic(schemas.first().homeId, it.id)
            val payload = objectMapper.writeValueAsBytes(it)
            mqttClient.publish(topic, payload, 0, true)
        }
    }

//    fun publishVolatile(homeId: Int, deviceId: String, property: String, payload: ByteArray) {
//        val topic = getPropertyTopic(homeId, deviceId, property)
//        mqttClient.publish(topic, payload, 0, false)
//    }

    fun publishMapData(homeId: Int, deviceId: String, mapData: MapDataForPublish) {
        mapData.getFields().forEach {
            val sectionData = mapData[it]
            val name = it.camelToSnakeCase()
            val topic = getPropertyTopic(homeId, deviceId, name)
            val payload = if (sectionData is String) {
                sectionData.toByteArray()
            } else {
                objectMapper.writeValueAsBytes(sectionData)
            }

            mqttClient.publish(topic, payload, 0, false)
        }
    }

    fun getHomeTopic(homeId: Int): String = HOME_TOPIC.replace("{$BASE_TOPIC}", bridgeMqttConfig.baseTopic)
        .replace("{$HOME_ID}", homeId.toString())

    fun getDeviceTopic(homeId: Int, deviceId: String): String = DEVICE_TOPIC.replace(HOME_TOPIC, getHomeTopic(homeId))
        .replace("{$DEVICE_ID}", deviceId)

    fun getRoutineTopic(homeId: Int, routineId: Int): String = ROUTINE_TOPIC.replace(HOME_TOPIC, getHomeTopic(homeId))
        .replace("{$ROUTINE_ID}", routineId.toString())


    fun getPropertyTopic(homeId: Int, deviceId: String, property: String): String =
        DEVICE_PROPERTY_TOPIC.replace(DEVICE_TOPIC, getDeviceTopic(homeId, deviceId))
            .replace("{$PROPERTY}", property)


//    fun getPropertyCommandTopic(homeId: Int, deviceId: String, property: String, cmd: String): String {
//        return DEVICE_PROPERTY_COMMAND_TOPIC.replace(
//            DEVICE_PROPERTY_TOPIC,
//            getPropertyTopic(homeId, deviceId, property)
//        ).replace("{$COMMAND}", cmd)
//    }

    fun getDeviceLogTopic(homeId: Int, deviceId: String): String {
        return DEVICE_LOG_TOPIC.replace(DEVICE_TOPIC, getDeviceTopic(homeId, deviceId))
    }

    fun getBridgeLogTopic(): String {
        return BRIDGE_LOG_TOPIC.replace("{$BASE_TOPIC}", bridgeMqttConfig.baseTopic)
    }

    @PreDestroy
    fun disconnect() {
        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    companion object {
        val logger by LoggerDelegate()

        const val BASE_TOPIC = "baseTopic"
        const val DEVICE = "device"
        const val DEVICE_ID = "deviceId"
        const val PROPERTY = "property"
        const val HOME = "home"
        const val HOME_ID = "homeId"
        const val ROUTINE = "routine"
        const val ROUTINE_ID = "routineId"
//        const val COMMAND = "command"

        //        const val ROOM = "room"
//        const val ROOM_ID = "roomId"
        const val LOG = "log"
        private const val HOME_TOPIC_PARTIAL = "$HOME/{$HOME_ID}"
        private const val DEVICE_TOPIC_PARTIAL = "$DEVICE/{$DEVICE_ID}"
        private const val ROUTINE_TOPIC_PARTIAL = "$ROUTINE/{$ROUTINE_ID}"

        //        const val ROOM_TOPIC_PARTIAL = "$ROOM/{$ROOM_ID}"
        const val HOME_TOPIC = "{$BASE_TOPIC}/$HOME_TOPIC_PARTIAL"
        const val ROOM_TOPIC = "$HOME_TOPIC/rooms"
        const val ROUTINE_TOPIC = "$HOME_TOPIC/$ROUTINE_TOPIC_PARTIAL"
        const val DEVICE_TOPIC = "$HOME_TOPIC/$DEVICE_TOPIC_PARTIAL"
        const val DEVICE_PROPERTY_TOPIC = "$DEVICE_TOPIC/{$PROPERTY}"

        //        const val DEVICE_PROPERTY_COMMAND_TOPIC = "$DEVICE_PROPERTY_TOPIC/{$COMMAND}"
        const val DEVICE_LOG_TOPIC = "$DEVICE_TOPIC/$LOG"
        const val BRIDGE_LOG_TOPIC = "{$BASE_TOPIC}/$LOG"
    }
}