package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.RoborockData
import de.konqi.roborockbridge.roborockbridge.protocol.helper.DeviceKeyMemory
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestData
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

@Component
class RoborockMqtt() {
    @Autowired
    private lateinit var roborockData: RoborockData

    @Autowired
    private lateinit var requestMemory: RequestMemory

    @Autowired
    private lateinit var deviceKeyMemory: DeviceKeyMemory

    @Autowired
    private lateinit var messageDecoder: MessageDecoder

    @Autowired
    private lateinit var request101Factory: Request101Factory

    private val username: String
        get() = ProtocolUtils.calcHexMd5(
            arrayOf(
                roborockData.rriot.userId,
                roborockData.rriot.mqttKey
            ).joinToString(":")
        ).substring(2, 10)
    private val password: String
        get() = ProtocolUtils.calcHexMd5(
            arrayOf(
                roborockData.rriot.sessionId,
                roborockData.rriot.mqttKey
            ).joinToString(":")
        ).substring(16)
    private val broker: String get() = roborockData.rriot.remote.mqttServer

    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${ProtocolUtils.CLIENT_ID_SHORT}_${ProtocolUtils.generateNonce()}"
    private val subscribeTopic: String get() = "rr/m/o/${roborockData.rriot.userId}/${username}/#"
    private val publishTopic: String get() = "rr/m/i/${roborockData.rriot.userId}/${username}/{deviceId}"

    private val persistence = MemoryPersistence()

    private lateinit var mqttClient: MqttClient

    fun monitorDevice(deviceId: String, key: String) {
        deviceKeyMemory[deviceId] = key

        // start polling loop
    }

    @Scheduled(fixedDelay = 10000)
    fun pollStatus() {
        deviceKeyMemory.keys.forEach { deviceId ->
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
                handleMessage(deviceId = device, payload = ByteBuffer.wrap(message.payload))
            } catch (e: Exception) {
                logger.error("Error while handling message from topic $topic", e)
            }
        }
    }

    fun handleMessage(deviceId: String, payload: ByteBuffer) {
        messageDecoder.decode(deviceKeyMemory[deviceId]!!, payload)

        // TODO: Forward response to mqtt
    }


    fun disconnect() {
        mqttClient.unsubscribe(subscribeTopic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    private fun publishStatusRequest(deviceId: String) {
        val deviceKey = deviceKeyMemory[deviceId]
        if (deviceKey != null) {
            val topic = publishTopic.replace("{deviceId}", deviceId)
            logger.info("Requesting status via topic $topic")
            val method = RequestMethod.GET_PROP

            val (requestId, message) = request101Factory.createRequest(
                key = deviceKey,
                method = method, parameters = arrayOf("get_status")
            )

            logger.trace("Request payload: ${String(message.payload)}")
            requestMemory[requestId.toInt()] = RequestData(method)
            mqttClient.publish(topic, MqttMessage(message.bytes))
        } else {
            logger.info("Unable to request status for $deviceId.")
        }
    }


    companion object {
        private val logger by LoggerDelegate()
    }
}
