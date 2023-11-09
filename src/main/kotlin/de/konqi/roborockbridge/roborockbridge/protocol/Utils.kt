package de.konqi.roborockbridge.roborockbridge.protocol

import org.springframework.security.crypto.codec.Hex
import java.security.MessageDigest
import kotlin.random.Random

class Utils {
    companion object {
        const val CLIENT_ID = "RoborockBridge"
        const val CLIENT_ID_SHORT = "rrb"
        private val NONCE_CHAR_POOL: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        @JvmStatic
        fun calcHexMd5(input: String): String {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update((input.toByteArray()))

            return String(Hex.encode(md5.digest()))
        }

        @JvmStatic
        fun generateNonce(length: Int = 6): String {
            return (1..length).map {
                Random.nextInt(0, NONCE_CHAR_POOL.size).let {
                    NONCE_CHAR_POOL[it]
                }
            }.joinToString("")
        }
    }
}