package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.Rriot
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class RoborockMqtt(rriot: Rriot) {
    private val username = Utils.calcHexMd5(arrayOf(rriot.userId, rriot.mqttKey).joinToString(":")).substring(2, 10)
    private val password = Utils.calcHexMd5(arrayOf(rriot.sessionId, rriot.mqttKey).joinToString(":")).substring(16)
    private val broker = rriot.remote.mqttServer

    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${Utils.CLIENT_ID_SHORT}_${Utils.generateNonce()}"
    private val topic = "rr/m/o/${rriot.userId}/${username}/#"

    private val persistence = MemoryPersistence()
    private val mqttClient = MqttClient(broker, clientId, persistence)

    fun connect() {
        val connectionOptions = MqttConnectOptions().also {
            it.keepAliveInterval = 60
            it.isCleanSession = true
            it.userName = username
            it.password = password.toCharArray()
        }
        val foo = null
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

        if (!mqttClient.isConnected) {
            throw RuntimeException("Unable to connect to mqtt broker")
        }

        mqttClient.subscribe(
            topic, 0
        ) { topic, message -> logger.info("Lambda: New message for topic '$topic' ${message.id}") }

//        mqttClient.
        // sendRequest(deviceId, 'get_prop', ['get_status'])
    }

    fun disconnect() {
        mqttClient.unsubscribe(topic)

        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    companion object {
        private val logger by LoggerDelegate()

        // @Todo this does not belong here
        private var idCounter = 1
        private val endpoint = Utils.generateNonce()
        private val nonce = Utils.generateNonce()

        // hardcoded in librrcodec.so, encrypted by the value of "com.roborock.iotsdk.appsecret" (see https://gist.github.com/rovo89/dff47ed19fca0dfdda77503e66c2b7c7)
        private const val RR_APPSECRET_SALT = "TXdfu\$jyZ#TZHsg4"

//        private fun sendRequest(
//            deviceId: String,
//            method: String,
//            params: List<String> = emptyList(),
//            secure: Boolean = false
//        ) {
//            val timestamp = Utils.getTimeSeconds()
//            val requestId = idCounter++
//            val body = mapOf("id" to requestId, "method" to method, "params" to params)
//            if (secure) {
//                body.plus("security" to mapOf("endpoint" to endpoint, "nonce" to nonce))
//            }
//            val serializedBody = Json.encodeToString(body)
//            val payload = mapOf("t" to timestamp, "dps": mapOf("101": serializedBody))
//            val serializedPayload = Json.encodeToString(payload)
//            sendRaw(deviceId, 101, timestamp, serializedPayload)
//        }

//        private fun encodeTimestamp(timestamp: Long): String {
//            return timestamp.toString(16).padStart(8, '0').split("").let {
//                arrayOf(5, 6, 3, 7, 1, 2, 0, 4).joinToString("") { swapIndex -> it[swapIndex] }
//            }
//        }

//        private fun sendRaw(deviceId: String, /* 101 */ protocol: Int, timestamp: Long, payload: String) {
//            val localKey = "" // from user-home find device and use localkey
//            val aesKey = Utils.calcMd5("${encodeTimestamp(timestamp)}${localKey}${RR_APPSECRET_SALT}")
//
//            val cipher = Cipher.getInstance("aes-128-ecb").also {
//                it.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey,"AES"), IvParameterSpec(Random.Default.nextBytes(16)))
//            }
//            cipher.update(payload)
//            cipher.doFinal()


//            const localKey = localKeys.get(deviceId);
//            const aesKey = md5bin(_encodeTimestamp(timestamp) + localKey + salt);
//            const cipher = crypto.createCipheriv('aes-128-ecb', aesKey, null);
//            const encrypted = Buffer.concat([cipher.update(payload), cipher.final()]);
//            const msg = Buffer.alloc(23 + encrypted.length);
//            msg.write('1.0');
//            msg.writeUint32BE(seq++ & 0xffffffff, 3);
//            msg.writeUint32BE(random++ & 0xffffffff, 7);
//            msg.writeUint32BE(timestamp, 11);
//            msg.writeUint16BE(protocol, 15);
//            msg.writeUint16BE(encrypted.length, 17);
//            encrypted.copy(msg, 19);
//            const crc32 = CRC32.buf(msg.subarray(0, msg.length - 4)) >>> 0;
//            msg.writeUint32BE(crc32, msg.length - 4);
//            client.publish(`rr/m/i/${rriot.u}/${mqttUser}/${deviceId}`, msg);
//        }

    }
}
