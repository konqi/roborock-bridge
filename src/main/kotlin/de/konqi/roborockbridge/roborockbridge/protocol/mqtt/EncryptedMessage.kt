package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.Utils
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

open class EncryptedMessage(private val key: String, raw: ByteArray? = null) : Message(raw) {
    override var payload: ByteArray
        get() = decode(super.payload)
        set(value) {
            super.payload = encode(value)
        }

    private val aesKey: ByteArray get() = Utils.calcMd5("${encodeTimestamp(header.timestamp.toLong())}${key}${RR_APPSECRET_SALT}")

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
        // hardcoded in librrcodec.so, encrypted by the value of "com.roborock.iotsdk.appsecret" (see https://gist.github.com/rovo89/dff47ed19fca0dfdda77503e66c2b7c7)
        private const val RR_APPSECRET_SALT = "TXdfu\$jyZ#TZHsg4"
        private const val CIPHER = "AES/ECB/PKCS5Padding"
        private val logger by LoggerDelegate()

        fun encodeTimestamp(timestamp: Long): String {
            return timestamp.toString(16).padStart(8, '0').let {
                arrayOf(5, 6, 3, 7, 1, 2, 0, 4).map { swapIndex -> it[swapIndex] }.joinToString("")
            }
        }
    }
}