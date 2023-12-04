package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.Command
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.persistence.RobotRepository
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestData
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import de.konqi.roborockbridge.roborockbridge.utility.CircularConcurrentLinkedQueue
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

@Component
class RoborockMqtt(
    @Autowired private val roborockCredentials: RoborockCredentials,
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val messageDecoder: MessageDecoder,
    @Autowired private val request101Factory: Request101Factory,
    private val robotRepository: RobotRepository
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

    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${ProtocolUtils.CLIENT_ID_SHORT}_${ProtocolUtils.generateNonce()}"
    private val subscribeTopic: String get() = "rr/m/o/${roborockCredentials.userId}/${username}/#"
    private val publishTopic: String get() = "rr/m/i/${roborockCredentials.userId}/${username}/{deviceId}"

    private val persistence = MemoryPersistence()

    private lateinit var mqttClient: MqttClient

    lateinit var thread: Thread

    val inboundMessagesQueue = CircularConcurrentLinkedQueue<Any>(20)

    @PostConstruct
    private fun init() {
        thread = Thread(this)
    }

    fun start() {
        thread.start()
    }

    @PreDestroy
    private fun onDestroy() {
//        run.set(false)

        disconnect()
    }

    override fun run() {
//        run.set(true)

        connect()
//        while (run.get()) {}
    }

    fun monitorDevice(deviceId: String) {
        // start polling loop
    }

    //    @Scheduled(fixedDelay = 10000)
    fun pollStatus() {
//        deviceKeyMemory.keys.forEach { deviceId ->
//            logger.debug("Polling $deviceId")
//            publishRequest(deviceId, RequestMethod.GET_PROP, listOf("get_status"))
//        }
    }

    private fun connect() {
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
        robotRepository.getByDeviceId(deviceId).ifPresent { robot ->
            val message = messageDecoder.decode(robot.deviceKey, payload)
            if (message != null) {
                if (!inboundMessagesQueue.offer(message)) {
                    logger.warn("Discarded message due to backpressure")
                }
            }
        }
    }


    private fun disconnect() {
        mqttClient.unsubscribe(subscribeTopic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    @Throws(RuntimeException::class)
    private fun publishRequest(deviceId: String, method: RequestMethod, parameters: List<String>? = null) {
        val robot = robotRepository.getByDeviceId(deviceId).orElseThrow {
            RuntimeException("No key available for device $deviceId")
        }

        val topic = publishTopic.replace("{deviceId}", deviceId)
        val (requestId, message) = if (parameters != null) request101Factory.createRequest(
            method = method,
            key = robot.deviceKey,
            parameters = parameters
        ) else request101Factory.createRequest(method = method, key = robot.deviceKey)
        mqttClient.publish(topic, MqttMessage(message.bytes))
        logger.info("Published  ${method.value} request via topic $topic")
        requestMemory[requestId.toInt()] = RequestData(method)
    }

    fun publishReturnToChargingStation(deviceId: String) {
        publishRequest(deviceId, RequestMethod.APP_CHARGE)
    }
//    private fun publishStatusRequest(deviceId: String) {
//        publishRequest(deviceId, RequestMethod.GET_PROP, listOf("get_status"))
//    }


    companion object {
        private val logger by LoggerDelegate()
    }
}
