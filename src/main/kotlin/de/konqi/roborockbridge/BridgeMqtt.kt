package de.konqi.roborockbridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import de.konqi.roborockbridge.bridge.DeviceForPublish
import de.konqi.roborockbridge.bridge.MapDataForPublish
import de.konqi.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.persistence.entity.Schema
import de.konqi.roborockbridge.utility.CircularConcurrentLinkedQueue
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
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

enum class CommandType(val value: String) {
    GET("get"), SET("set"), ACTION("action");

    companion object {
        private val mapping = CommandType.entries.associateBy(CommandType::value)
        fun fromValue(value: String) = CommandType.mapping[value]
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
    START_SCHEMA("start_schema"),
    UNKNOWN("unknown");

    companion object {
        private val mapping = ActionEnum.entries.associateBy(ActionEnum::value)
        fun fromValue(value: String?) = ActionEnum.mapping[value] ?: UNKNOWN
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

class ActionCommand(target: Target, val action: ActionEnum, val arguments: Map<String, String> = emptyMap()) :
    Command(target)

class GetCommand(target: Target, val parameters: List<String> = emptyList()) : Command(target)


@ConfigurationProperties(prefix = "bridge-mqtt")
data class BridgeMqttConfig(
    val url: String, val clientId: String, val baseTopic: String
)

@Component
@Profile("bridge")
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
        "${Regex.escape(bridgeMqttConfig.baseTopic)}(?:/(?:${
            sectionRegex(
                HOME,
                "[0-9]+"
            )
        }|${sectionRegex(DEVICE)}))*(?:/(?<surplus>.*))?"
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
        if(!CommandType.entries.any {
            topic.endsWith(it.value)
        }) {
            logger.debug("Topic '$topic' is not a command, ignoring it.")
            return
        }

        val topicParams = parseTopic(topic)
        val homeId = topicParams[HOME_ID]?.toInt()
        val deviceId = topicParams[DEVICE_ID]
        val surplus = topicParams["surplus"] ?: ""
        val cmdStr = surplus.split("/").last()
        val cmd = CommandType.Companion.fromValue(cmdStr)
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
                val args = messageBody?.get("args")

                inboundMessagesQueue.add(
                    ActionCommand(
                        action = ActionEnum.Companion.fromValue(
                            messageBody?.get("action")?.textValue()
                        ),
                        arguments = if (args != null) objectMapper.treeToValue<Map<String, String>>(args) else emptyMap(),
                        target = Target(
                            type = TargetType.DEVICE,
                            identifier = deviceId
                        )
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

                inboundMessagesQueue.add(
                    GetCommand(
                        target = Target(type = targetType, identifier = targetIdentifier),
                        parameters = properties
                    )
                )
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

    fun announceDevice(device: DeviceForPublish) {
        logger.info("Announcing new robot with id '${device.deviceId}'")
        val topic = getDeviceTopic(homeId = device.homeId, deviceId = device.deviceId)
        val payload = objectMapper.writeValueAsBytes(device)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun announceRooms(rooms: List<Room>) {
        logger.info("Announcing ${rooms.size} rooms.")
        val topic = ROOM_TOPIC.replace(Companion.HOME_TOPIC, getHomeTopic(rooms.first().home.homeId))
        val payload = objectMapper.writeValueAsBytes(rooms)
        mqttClient.publish(
            topic, payload, 0, true
        )
    }

    fun announceSchemas(schemas: List<Schema>) {
        logger.info("Announcing ${schemas.size} schemas.")
        val topic = SCHEMA_TOPIC.replace(Companion.HOME_TOPIC, getHomeTopic(schemas.first().home.homeId))
        val payload = objectMapper.writeValueAsBytes(schemas)
        mqttClient.publish(topic, payload, 0, true)
    }

    fun publishVolatile(homeId: Int, deviceId: String, property: String, payload: ByteArray) {
        val topic = getPropertyTopic(homeId, deviceId, property)
        mqttClient.publish(topic, payload, 0, false)
    }

    fun publishMapData(homeId: Int, deviceId: String, payload: MapDataForPublish) {
        val topic = getPropertyTopic(homeId, deviceId, "map")
        mqttClient.publish(topic, objectMapper.writeValueAsBytes(payload), 0, false)
    }


    fun getHomeTopic(homeId: Int): String {
        return Companion.HOME_TOPIC.replace("{$BASE_TOPIC}", bridgeMqttConfig.baseTopic).replace("{$HOME_ID}", homeId.toString())
    }

    fun getDeviceTopic(homeId: Int, deviceId: String): String {
        return Companion.DEVICE_TOPIC.replace(Companion.HOME_TOPIC, getHomeTopic(homeId)).replace("{$DEVICE_ID}", deviceId)
    }

    fun getPropertyTopic(homeId: Int, deviceId: String, property: String): String {
        return Companion.DEVICE_PROPERTY_TOPIC.replace(Companion.DEVICE_TOPIC, getDeviceTopic(homeId, deviceId))
            .replace("{$PROPERTY}", property)
    }

    fun getPropertyCommandTopic(homeId: Int, deviceId: String, property: String, cmd: String): String {
        return Companion.DEVICE_PROPERTY_COMMAND_TOPIC.replace(
            Companion.DEVICE_PROPERTY_TOPIC,
            getPropertyTopic(homeId, deviceId, property)
        ).replace("{$COMMAND}", cmd)
    }

    fun getDeviceLogTopic(homeId: Int, deviceId: String): String {
        return Companion.DEVICE_LOG_TOPIC.replace(Companion.DEVICE_TOPIC, getDeviceTopic(homeId, deviceId))
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
        const val HOME_TOPIC = "{$BASE_TOPIC}/${Companion.HOME_TOPIC_PARTIAL}"
        const val ROOM_TOPIC = "${Companion.HOME_TOPIC}/rooms"
        const val SCHEMA_TOPIC = "${Companion.HOME_TOPIC}/schemas"
        const val DEVICE_TOPIC = "${Companion.HOME_TOPIC}/${Companion.DEVICE_TOPIC_PARTIAL}"
        const val DEVICE_PROPERTY_TOPIC = "${Companion.DEVICE_TOPIC}/{$PROPERTY}"
        const val DEVICE_PROPERTY_COMMAND_TOPIC = "${Companion.DEVICE_PROPERTY_TOPIC}/{$COMMAND}"
        const val DEVICE_LOG_TOPIC = "${Companion.DEVICE_TOPIC}/$LOG"
        const val BRIDGE_LOG_TOPIC = "{$BASE_TOPIC}/$LOG"
    }
}