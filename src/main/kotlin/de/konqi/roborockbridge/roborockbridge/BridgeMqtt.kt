package de.konqi.roborockbridge.roborockbridge

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Robot
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.roborockbridge.utility.CircularConcurrentLinkedQueue
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

enum class CommandType(val id: String) {
    GET("get"), SET("set"), ACTION("action");

    companion object {
        private val mapping = CommandType.entries.associateBy(CommandType::id)
        fun fromValue(value: String) = mapping[value]
    }
}

data class Message(
    val topic: String,
    val message: ByteArray,
    val qos: Int = 0,
    val retained: Boolean = false
)

enum class ActionEnum(val value: String) {
    HOME("home"),
    UNKNOWN("unknown");

    companion object {
        private val mapping = ActionEnum.entries.associateBy(ActionEnum::value)
        fun fromValue(value: String?) = mapping[value] ?: UNKNOWN
    }
}

// need to specify target bridge / home / device
enum class TargetType {
    BRIDGE,
    HOME,
    DEVICE
}

data class Target(val type: TargetType, val identifier: String?)

abstract class Command(
    val target: Target
)

class ActionCommand(target: Target, val action: ActionEnum) : Command(target)
class GetCommand(target: Target) : Command(target)


@ConfigurationProperties(prefix = "bridge-mqtt")
data class BridgeMqttConfig(
    val url: String, val clientId: String, val baseTopic: String
)

@Component
@EnableConfigurationProperties(BridgeMqttConfig::class)
class BridgeMqtt(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig, @Autowired private val objectMapper: ObjectMapper
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
                    logger.info("New message for topic '$topic'")
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {
                    logger.warn("Message delivered")
                }
            })

            connect(MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                isAutomaticReconnect = true
            })

            subscribe("${bridgeMqttConfig.baseTopic}/#", 0) { topic, message ->
                try {
                    handleMessage(topic, message)
                } catch (e: Exception) {
                    logger.error("Error while processing message: ${e.message}, Stacktrace: ${e.printStackTrace()}")
                }
            }
        }
    }

    private final fun sectionRegex(sectionName: String, valuePattern: String = "[^/]+"): String {
        return "(?:(?:$sectionName)/(?<$sectionName>$valuePattern))"
    }

    val deviceIdExtractionRegex = Regex(
        "${Regex.escape(bridgeMqttConfig.baseTopic)}(?:/(?:${sectionRegex(HOME, "[0-9]+")}|${sectionRegex(DEVICE)}))*(?:/(?<surplus>.*))?"
    )

    fun parseTopic(topic: String): Map<String, String?> {
        val matches = deviceIdExtractionRegex.find(topic)

        return mapOf(
            HOME_ID to matches?.groups?.get(HOME)?.value,
            DEVICE_ID to matches?.groups?.get(DEVICE)?.value,
            "surplus" to matches?.groups?.get("surplus")?.value
        )
    }

    fun handleMessage(topic: String, message: MqttMessage) {
        if (topic.endsWith("log")) return

        val topicParams = parseTopic(topic)
        val homeId = topicParams[HOME_ID]?.toInt()
        val deviceId = topicParams[DEVICE_ID]
        val surplus = topicParams["surplus"] ?: ""
        val cmdStr = surplus.split("/").last()
        val cmd = CommandType.fromValue(cmdStr)
        val properties = surplus.split("/").let {
            val indexOfParams = it.indexOf(deviceId) + 1
            it.slice(indexOfParams..<it.size - 1)
        }
        val messageBody = try {
            objectMapper.readTree(message.payload)
        } catch (e: Exception) {
            // notify about invalid message
            log("Could not parse message '${String(message.payload)}'.", homeId, deviceId)
            null
        }

        logger.info("Received message for home '$homeId', device '$deviceId' with command '$cmd'")

        when (cmd) {
            CommandType.ACTION -> {
                inboundMessagesQueue.add(
                    ActionCommand(
                        action = ActionEnum.fromValue(messageBody?.get("action")?.textValue()),
                        target = Target(type = TargetType.DEVICE, identifier = deviceId)
                    )
                )
            }

            CommandType.GET -> {
                val (targetType, targetIdentifier) = if (deviceId != null) {
                    TargetType.DEVICE to deviceId
                } else if (homeId != null) {
                    TargetType.HOME to homeId.toString()
                } else {
                    TargetType.BRIDGE to null
                }

                inboundMessagesQueue.add(GetCommand(target = Target(type = targetType, identifier = targetIdentifier)))
            }

            else -> {
                log("Received unknown command '$cmdStr'.")
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

    fun announceDevice(robot: Robot) {
        logger.info("Announcing new robot with id '${robot.deviceId}'")
        val topic = getDeviceTopic(homeId = robot.home.homeId, deviceId = robot.deviceId)
        val payload = objectMapper.writeValueAsBytes(robot)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun announceRooms(rooms: List<Room>) {
        logger.info("Announcing ${rooms.size} rooms.")
        val topic = ROOM_TOPIC.replace(HOME_TOPIC, getHomeTopic(rooms[0].home.homeId))
        val payload = objectMapper.writeValueAsBytes(rooms)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun getHomeTopic(homeId: Int): String {
        return HOME_TOPIC.replace("{$BASE_TOPIC}", bridgeMqttConfig.baseTopic).replace("{$HOME_ID}", homeId.toString())
    }

    fun getDeviceTopic(homeId: Int, deviceId: String): String {
        return DEVICE_TOPIC.replace(HOME_TOPIC, getHomeTopic(homeId)).replace("{$DEVICE_ID}", deviceId)
    }

    fun getPropertyTopic(homeId: Int, deviceId: String, property: String): String {
        return DEVICE_PROPERTY_TOPIC.replace(DEVICE_TOPIC, getDeviceTopic(homeId, deviceId))
            .replace("{$PROPERTY}", property)
    }

    fun getPropertyCommandTopic(homeId: Int, deviceId: String, property: String, cmd: String): String {
        return DEVICE_PROPERTY_COMMAND_TOPIC.replace(
            DEVICE_PROPERTY_TOPIC,
            getPropertyTopic(homeId, deviceId, property)
        ).replace("{$COMMAND}", cmd)
    }

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
        const val COMMAND = "command"
        const val HOME = "home"
        const val HOME_ID = "homeId"

        //        const val ROOM = "room"
//        const val ROOM_ID = "roomId"
        const val LOG = "log"
        const val HOME_TOPIC_PARTIAL = "$HOME/{$HOME_ID}"
        const val DEVICE_TOPIC_PARTIAL = "$DEVICE/{$DEVICE_ID}"

        //        const val ROOM_TOPIC_PARTIAL = "$ROOM/{$ROOM_ID}"
        const val HOME_TOPIC = "{$BASE_TOPIC}/$HOME_TOPIC_PARTIAL"
        const val ROOM_TOPIC = "$HOME_TOPIC/rooms"
        const val DEVICE_TOPIC = "$HOME_TOPIC/$DEVICE_TOPIC_PARTIAL"
        const val DEVICE_PROPERTY_TOPIC = "$DEVICE_TOPIC/{$PROPERTY}"
        const val DEVICE_PROPERTY_COMMAND_TOPIC = "$DEVICE_PROPERTY_TOPIC/{$COMMAND}"
        const val DEVICE_LOG_TOPIC = "$DEVICE_TOPIC/$LOG"
        const val BRIDGE_LOG_TOPIC = "{$BASE_TOPIC}/$LOG"
    }
}