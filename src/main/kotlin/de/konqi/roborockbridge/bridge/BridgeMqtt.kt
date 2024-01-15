package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.bridge.dto.*
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
import org.springframework.stereotype.Component

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
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val receivedMessageParser: ReceivedMessageParser
) {
    val mqttClient = MqttClient(bridgeMqttConfig.url, bridgeMqttConfig.clientId, null)
    val inboundMessagesQueue = CircularConcurrentLinkedQueue<ReceivedMessage>(20)

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
                    logger.debug("Message delivered (${token?.messageId})")
                }
            })

            try {
                connect(MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    isAutomaticReconnect = true

                    if (!bridgeMqttConfig.username.isNullOrBlank()) {
                        userName = bridgeMqttConfig.username
                    }
                    if (!bridgeMqttConfig.password.isNullOrBlank()) {
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
        val msg = receivedMessageParser.parse(topic, message.payload) ?: return
        inboundMessagesQueue.add(msg)
    }

    fun announceHome(home: HomeForPublish) {
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

    fun announceRooms(rooms: List<RoomForPublish>) {
        logger.info("Announcing ${rooms.size} rooms.")
        val topic = ROOM_TOPIC.replace(HOME_TOPIC, getHomeTopic(rooms.first().homeId))
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
        const val DEVICE_PROPERTY = "property"
        const val DEVICE_ID = "deviceId"
        const val PROPERTY = "property"
        const val HOME = "home"
        const val HOME_ID = "homeId"
        const val ROUTINE = "routine"
        const val ROUTINE_ID = "routineId"

        private const val HOME_TOPIC_PARTIAL = "$HOME/{$HOME_ID}"
        private const val DEVICE_TOPIC_PARTIAL = "$DEVICE/{$DEVICE_ID}"
        private const val ROUTINE_TOPIC_PARTIAL = "$ROUTINE/{$ROUTINE_ID}"

        const val HOME_TOPIC = "{$BASE_TOPIC}/$HOME_TOPIC_PARTIAL"
        const val ROOM_TOPIC = "$HOME_TOPIC/rooms"
        const val ROUTINE_TOPIC = "$HOME_TOPIC/$ROUTINE_TOPIC_PARTIAL"
        const val DEVICE_TOPIC = "$HOME_TOPIC/$DEVICE_TOPIC_PARTIAL"
        const val DEVICE_PROPERTY_TOPIC = "$DEVICE_TOPIC/{$PROPERTY}"
    }
}