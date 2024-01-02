package de.konqi.roborockbridge.remote.mqtt.raw

import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.remote.ProtocolUtils
import de.konqi.roborockbridge.remote.mqtt.RoborockMqttConfiguration
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

open class EncryptedMessage : Message {
    private val key: String

    constructor(key: String) : super() {
        this.key = key
    }

    constructor(key: String, buffer: ByteBuffer) : super(buffer) {
        this.key = key
    }

    override var payload: ByteArray
        get() = decode(super.payload)
        set(value) {
            super.payload = encode(value)
        }

    private val aesKey: ByteArray get() = ProtocolUtils.calcMd5("${encodeTimestamp(header.timestamp.toLong())}${key}$RR_APP_SECRET_SALT")

    private fun decode(payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER).also {
            it.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(aesKey, "AES")
            )
        }

        return cipher.doFinal(payload)
    }

    private fun encode(payload: ByteArray): ByteArray {
        val length = payload.size
        val cipher = Cipher.getInstance(CIPHER).also {
            it.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        }

        val encoded = cipher.doFinal(payload)
        logger.debug("Payload length before encryption: $length, after: ${encoded.size}")
        return encoded
    }

    companion object {
        // injected via the initializer
        lateinit var RR_APP_SECRET_SALT: String
        private const val CIPHER = "AES/ECB/PKCS5Padding"
        private val logger by LoggerDelegate()

        fun encodeTimestamp(timestamp: Long): String {
            return timestamp.toString(16).padStart(8, '0').let {
                arrayOf(5, 6, 3, 7, 1, 2, 0, 4).map { swapIndex -> it[swapIndex] }.joinToString("")
            }
        }
    }
}

@Component
class EncryptedMessageInitializer(@Autowired private val roborockMqttConfiguration: RoborockMqttConfiguration) {
    @PostConstruct
    fun postConstruct() {
        EncryptedMessage.RR_APP_SECRET_SALT = roborockMqttConfiguration.appSecretSalt
    }
}