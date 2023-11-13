package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import de.konqi.roborockbridge.roborockbridge.protocol.Utils
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptedMessage(private val key: String, raw: ByteArray? = null) : Message(raw) {

    override var payload: ByteArray
        get() = decode(super.payload)
        set(value) {
            super.payload = encode(value)
        }

    /**
     * @param localKey from user-home find device and use localkey
     */
    private fun decode(payload: ByteArray): ByteArray {
        val aesKey = Utils.calcMd5("${encodeTimestamp(header.timestamp.toLong())}${key}${RR_APPSECRET_SALT}")

        val cipher = Cipher.getInstance(CIPHER).also {
            it.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES")) // , IvParameterSpec(Random.Default.nextBytes(16))
        }

        return cipher.doFinal(payload)
    }

    private fun encode(payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER).also {
            val aesKey = Utils.calcMd5("${encodeTimestamp(header.timestamp.toLong())}${key}${RR_APPSECRET_SALT}")

            it.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES")) // , IvParameterSpec(ByteArray(16) { 0 })
        }

        return cipher.doFinal(payload)
    }

    companion object {
        // hardcoded in librrcodec.so, encrypted by the value of "com.roborock.iotsdk.appsecret" (see https://gist.github.com/rovo89/dff47ed19fca0dfdda77503e66c2b7c7)
        private const val RR_APPSECRET_SALT = "TXdfu\$jyZ#TZHsg4"
        private const val CIPHER = "AES/ECB/PKCS5Padding" // (128)

        private fun encodeTimestamp(timestamp: Long): String {
            return timestamp.toString(16).padStart(8, '0').split("").let {
                arrayOf(5, 6, 3, 7, 1, 2, 0, 4).joinToString("") { swapIndex -> it[swapIndex] }
            }
        }
    }
}