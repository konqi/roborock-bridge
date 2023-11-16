package de.konqi.roborockbridge.roborockbridge.protocol

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.RoborockData
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RoborockMqtt() {
    @Autowired
    private lateinit var roborockData: RoborockData

    private val username: String
        get() = Utils.calcHexMd5(
            arrayOf(
                roborockData.rriot.userId,
                roborockData.rriot.mqttKey
            ).joinToString(":")
        ).substring(2, 10)
    private val password: String
        get() = Utils.calcHexMd5(
            arrayOf(
                roborockData.rriot.sessionId,
                roborockData.rriot.mqttKey
            ).joinToString(":")
        ).substring(16)
    private val broker: String get() = roborockData.rriot.remote.mqttServer

    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${Utils.CLIENT_ID_SHORT}_${Utils.generateNonce()}"
    private val subscribeTopic: String get() = "rr/m/o/${roborockData.rriot.userId}/${username}/#"
    private val publishTopic: String get() = "rr/m/i/${roborockData.rriot.userId}/${username}/{deviceId}"

    private val deviceKeyMap = HashMap<String, String>()

    private val persistence = MemoryPersistence()

    private lateinit var mqttClient: MqttClient

    fun monitorDevice(deviceId: String, key: String) {
        deviceKeyMap[deviceId] = key

        // start polling loop
    }

    @Scheduled(fixedDelay = 10000)
    fun pollStatus() {
        deviceKeyMap.keys.forEach { deviceId ->
            logger.debug("Polling $deviceId")
            publishStatusRequest(deviceId)
        }
    }

    fun connect() {
        mqttClient = MqttClient(broker, clientId, persistence)

        val connectionOptions = MqttConnectOptions().also {
            it.keepAliveInterval = 60
            it.isCleanSession = true
            it.userName = username
            it.password = password.toCharArray()
        }
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                logger.warn("Connection lost")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                logger.info("New message for topic '$topic'")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                logger.warn("Message delivered")
            }

        })

        mqttClient.connect(connectionOptions)

        subscribe()
    }

    private fun subscribe() {
        if (!mqttClient.isConnected) {
            throw RuntimeException("Unable to connect to mqtt broker")
        }

        logger.info("Subscribing to topic $subscribeTopic")
        mqttClient.subscribe(
            subscribeTopic, 0
        ) { topic, message ->
            try {
                logger.info("Lambda: New message for topic '$topic' ${message.id}")
                val device = topic.substring(topic.lastIndexOf('/') + 1)
                handleMessage(deviceId = device, payload = message.payload)
            } catch (e: Exception) {
                logger.error("Error while handling message from topic $topic", e)
            }
        }
    }

    fun handleMessage(deviceId: String, payload: ByteArray) {
        val data = EncryptedMessage(deviceKeyMap[deviceId]!!, payload)
        val protocol = "${data.header.protocol}"
        logger.info("Protocol ${protocol}, message: ${String(data.payload)}")

        val mqttResponse: MqttResponse = objectMapper.readValue(data.payload)
        mqttResponse.dps.keys.all { it == protocol }
        val body = mqttResponse.dps[protocol]
        if (body != null) {
            val response102: Response102 = objectMapper.readValue(body)
            logger.info("${response102.result[0].common_status}")
            // TODO: Match response to request?
            // TODO: Forward reponse to mqtt
        }
    }


    fun disconnect() {
        mqttClient.unsubscribe(subscribeTopic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    private fun publishStatusRequest(deviceId: String) {
        val deviceKey = deviceKeyMap[deviceId]
        if (deviceKey != null) {
            val topic = publishTopic.replace("{deviceId}", deviceId)
            logger.info("Requesting status via topic $topic")

            val request = Request101(
                key = deviceKey,
                method = RequestMethodEnum.GET_PROP, parameters = arrayOf("get_status")
            )

            logger.trace("Request payload: ${String(request.payload)}")
            mqttClient.publish(topic, MqttMessage(request.bytes))
        } else {
            logger.info("Unable to request status for $deviceId.")
        }
    }

    companion object {
        private val logger by LoggerDelegate()
        private val objectMapper = jacksonObjectMapper()
    }
}
