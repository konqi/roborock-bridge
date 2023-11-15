package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.RoborockData
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.Request101
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.RequestMethodEnum
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Autowired
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

    private val persistence = MemoryPersistence()

    private lateinit var mqttClient: MqttClient

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
    }

    fun subscribe() {
        if (!mqttClient.isConnected) {
            throw RuntimeException("Unable to connect to mqtt broker")
        }

        logger.info("Subscribing to topic $subscribeTopic")
        mqttClient.subscribe(
            subscribeTopic, 0
        ) { topic, message -> logger.info("Lambda: New message for topic '$topic' ${message.id}") }
    }

    fun disconnect() {
        mqttClient.unsubscribe(subscribeTopic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    fun publishStatusRequest(deviceId: String, deviceLocalKey: String) {
        val topic = publishTopic.replace("{deviceId}", deviceId)
        logger.info("Requesting status via topic $topic")

        val request = Request101(
            key = deviceLocalKey,
            method = RequestMethodEnum.GET_PROP, parameters = arrayOf("get_status")
        )
        logger.info("Request payload: ${String(request.payload)}")
        mqttClient.publish(topic, MqttMessage(request.bytes))
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}
