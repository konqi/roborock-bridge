package de.konqi.roborockbridge.roborockbridge.protocol

import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

@Component
class ProtocolUtils {
    companion object {
        const val CLIENT_ID = "RoborockBridge"
        const val CLIENT_ID_SHORT = "rrb"
        private val NONCE_CHAR_POOL: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        @JvmStatic
        fun calcMd5(input: String): ByteArray {
            val md5 = MessageDigest.getInstance("MD5")
            return md5.digest(input.toByteArray())
        }

        @JvmStatic
        fun calcHexMd5(input: String): String {
            return String(Hex.encode(calcMd5(input)))
        }

        @JvmStatic
        fun generateNonce(length: Int = 6): String {
            return (1..length).map {
                Random.nextInt(0, NONCE_CHAR_POOL.size).let {
                    NONCE_CHAR_POOL[it]
                }
            }.joinToString("")
        }

        @JvmStatic
        fun getTimeSeconds(): Long {
            return Date().time / 1000
        }
    }
}