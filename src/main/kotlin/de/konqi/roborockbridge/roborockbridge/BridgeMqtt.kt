package de.konqi.roborockbridge.roborockbridge

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

enum class BridgeCommand(val id: String) {
    GET("get"),
    SET("set"),
    ACTION("action");

    companion object {
        private val mapping = BridgeCommand.entries.associateBy(BridgeCommand::id)
        fun fromValue(value: String) = mapping[value]
    }
}

@ConfigurationProperties(prefix = "bridge-mqtt")
data class BridgeMqttConfig(
    val url: String,
    val clientId: String,
    val baseTopic: String
)

@Component
@EnableConfigurationProperties(BridgeMqttConfig::class)
class BridgeMqtt(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig,
    @Autowired private val objectMapper: ObjectMapper
) {
    val mqttClient = MqttClient(bridgeMqttConfig.url, bridgeMqttConfig.clientId).also {
        it.connect()

        it.subscribe("${bridgeMqttConfig.baseTopic}/#", 0) { topic, message ->
            try {
                handleMessage(topic, message)
            } catch (e: Exception) {
                logger.error("Error while processing message: ${e.message}, Stacktrace: ${e.stackTrace}")
            }
        }
    }
    val deviceIdExtractionRegex = Regex("${Regex.escape(bridgeMqttConfig.baseTopic)}/([^/]+)/")

    fun handleMessage(topic: String, message: MqttMessage) {
        val deviceId = deviceIdExtractionRegex.find(topic)?.groupValues?.get(1)
        val cmd = BridgeCommand.fromValue(topic.split("/").last())
        val properties = topic.split("/").let {
            val indexOfParams = it.indexOf(deviceId) + 1
            it.slice(indexOfParams..<it.size - 1)
        }

        if (cmd != null && deviceId != null) {
            println(cmd)
            println(properties)
            when(cmd) {
                BridgeCommand.ACTION -> {}
                BridgeCommand.GET -> {}
                BridgeCommand.SET -> {}
            }
        }
//      val node = objectMapper.readTree(message.payload)


    }

//    @Scheduled(fixedDelay = 1000)
//    fun foo() {
//        mqttClient.publish("${bridgeMqttConfig.baseTopic}/hello", "Hello World!".toByteArray(), 0, false)
//    }

    fun announceDevice(deviceId: String) {
        val topic = getDeviceTopic(deviceId = deviceId)
        mqttClient.publish(
            topic, "Hi!".toByteArray(), 0, true
        )
    }

    fun getDeviceTopic(deviceId: String): String {
        return DEVICE_TOPIC
            .replace("{$BASE_TOPIC}", bridgeMqttConfig.baseTopic)
            .replace("{$DEVICE_ID}", deviceId)
    }

    fun getPropertyTopic(deviceId: String, property: String): String {
        return DEVICE_PROPERTY_TOPIC
            .replace(DEVICE_TOPIC, getDeviceTopic(deviceId))
            .replace("{$PROPERTY}", property)
    }

    fun getPropertyCommandTopic(deviceId: String, property: String, cmd: String): String {
        return DEVICE_PROPERTY_COMMAND_TOPIC
            .replace(DEVICE_PROPERTY_TOPIC, getPropertyTopic(deviceId, property))
            .replace("{$COMMAND}", cmd)
    }

    fun getDeviceLogTopic(deviceId: String): String {
        return DEVICE_LOG_TOPIC.replace(DEVICE_TOPIC, getDeviceTopic(deviceId))
    }

    @PreDestroy
    fun disconnect() {
        mqttClient.disconnect()
    }

    companion object {
        val logger by LoggerDelegate()

        const val BASE_TOPIC = "baseTopic"
        const val DEVICE_ID = "deviceId"
        const val PROPERTY = "property"
        const val COMMAND = "command"
        const val LOG = "log"
        const val DEVICE_TOPIC = "{$BASE_TOPIC}/{$DEVICE_ID}"
        const val DEVICE_PROPERTY_TOPIC = "$DEVICE_TOPIC/{$PROPERTY}"
        const val DEVICE_PROPERTY_COMMAND_TOPIC = "$DEVICE_PROPERTY_TOPIC/{$COMMAND}"
        const val DEVICE_LOG_TOPIC = "$DEVICE_TOPIC/$LOG"
    }
}