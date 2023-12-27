package de.konqi.roborockbridge.remote.mqtt

import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.persistence.DeviceRepository
import de.konqi.roborockbridge.remote.ProtocolUtils
import de.konqi.roborockbridge.remote.RoborockCredentials
import de.konqi.roborockbridge.remote.helper.RequestData
import de.konqi.roborockbridge.remote.helper.RequestMemory
import de.konqi.roborockbridge.remote.mqtt.*
import de.konqi.roborockbridge.utility.CircularConcurrentLinkedQueue
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

@Component
@Profile("bridge")
class RoborockMqtt(
    @Autowired private val roborockCredentials: RoborockCredentials,
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val messageDecoder: MessageDecoder,
    @Autowired private val request101Factory: Request101Factory,
    private val robotRepository: DeviceRepository
) : Runnable {
    private val username: String
        get() = ProtocolUtils.calcHexMd5(
            arrayOf(
                roborockCredentials.userId,
                roborockCredentials.mqttKey
            ).joinToString(":")
        ).substring(2, 10)

    private val password: String
        get() = ProtocolUtils.calcHexMd5(
            arrayOf(
                roborockCredentials.sessionId,
                roborockCredentials.mqttKey
            ).joinToString(":")
        ).substring(16)
    private val broker: String get() = roborockCredentials.mqttServer!!

    private val mqttConnectOptions
        get() = MqttConnectOptions().also {
            it.keepAliveInterval = 60
            it.isCleanSession = true
            it.userName = username
            it.password = password.toCharArray()
        }

    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${ProtocolUtils.CLIENT_ID_SHORT}_${ProtocolUtils.generateNonce()}"
    private val subscribeTopic: String get() = "rr/m/o/${roborockCredentials.userId}/${username}/#"
    private val publishTopic: String get() = "rr/m/i/${roborockCredentials.userId}/${username}/{deviceId}"

    private val persistence = MemoryPersistence()

    private lateinit var mqttClient: MqttClient

    lateinit var thread: Thread

    val inboundMessagesQueue = CircularConcurrentLinkedQueue<DecodedMqttMessage>(20)

    @PostConstruct
    private fun init() {
        thread = Thread(this)
    }

    fun start() {
        thread.start()
    }

    override fun run() {
        connect()
        subscribe()
    }

    @PreDestroy
    private fun disconnect() {
        mqttClient.unsubscribe(subscribeTopic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
        logger.info("disconnected")
    }


    val isAlive = AtomicBoolean(true)

    @Scheduled(fixedDelay = 30_000)
    private fun aliveCheck() {
        if (isAlive.get()) {
            isAlive.set(false)
        } else {
            if (mqttClient.isConnected) {
                logger.info("disconnecting since bus idle")
                // disconnect
                disconnect()
            }
        }
    }

    private fun alive() {
        if (!isAlive.get()) {
            // connect
            connect()
            subscribe()

            isAlive.set(true)
        }
    }

    private fun connect() {
        mqttClient = MqttClient(broker, clientId, persistence).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    logger.warn("Connection lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    alive()
                    logger.debug("New message for topic '$topic'")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    alive()
                    logger.debug("Message delivered")
                }
            })
        }

        mqttClient.connect(mqttConnectOptions)
        logger.info("connected")
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
        val messages = messageDecoder.decode(deviceId, payload)
        if (!inboundMessagesQueue.addAll(messages)) {
            logger.warn("Discarded message due to backpressure")
        }
    }

    @Throws(RuntimeException::class)
    fun publishRequest(
        deviceId: String,
        method: RequestMethod,
        parameters: List<String>? = null,
        secure: Boolean = false
    ) {
        alive()
        val robot = robotRepository.findById(deviceId).orElseThrow {
            RuntimeException("No key available for device $deviceId")
        }

        val topic = publishTopic.replace("{deviceId}", deviceId)
        val (requestId, message) = if (parameters != null) request101Factory.createRequest(
            method = method,
            key = robot.deviceKey,
            parameters = parameters,
            secure = secure
        ) else request101Factory.createRequest(method = method, key = robot.deviceKey, secure = secure)
        mqttClient.publish(topic, MqttMessage(message.bytes))
        logger.info("Published '${method.value}' request via topic '$topic'.")
        requestMemory.put(
            requestId.toInt(), RequestData(
                method = method,
                nonce = if (secure) request101Factory.generateNonce(requestId) else null
            )
        )
    }

    fun publishStatusRequest(deviceId: String) {
        alive()
        publishRequest(deviceId = deviceId, method = RequestMethod.GET_PROP, parameters = listOf("get_status"))
    }

    fun publishMapRequest(deviceId: String) {
        alive()
        publishRequest(deviceId = deviceId, method = RequestMethod.GET_MAP_V1, secure = true)
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}
