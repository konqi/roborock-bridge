package de.konqi.roborockbridge.protocol.helper

import de.konqi.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.protocol.mqtt.RequestMethod
import org.springframework.stereotype.Component
import java.util.*
import kotlin.collections.LinkedHashMap

data class RequestData(
    val method: RequestMethod,
    val nonce: ByteArray? = null,
    val requestTimeMs: Long = Date().time,
)

@Component
class RequestMemory {
    val memory = LinkedHashMap<Int, RequestData>(MAX_MEMORY_SIZE)

    fun put(key: Int, value: RequestData): RequestData? {
        while (memory.size >= MAX_MEMORY_SIZE) {
            val lost = memory.pollLastEntry()
            logger.warn(
                "Request {} with method '{}' evicted after {} ms",
                lost.key,
                lost.value.method,
                Date().time - lost.value.requestTimeMs
            )
        }
        return memory.put(key, value)
    }

    fun remove(key: Int) = memory.remove(key)

    fun get(key: Int) = memory.get(key)

    fun getAndRemove(key: Int): RequestData? {
        val value = memory.get(key)
        if (value != null) {
            logger.debug(
                "Request {} with method '{}' finished after {} ms",
                key,
                value.method,
                Date().time - value.requestTimeMs
            )
            memory.remove(key)
        }

        return value
    }

    companion object {
        private val logger by LoggerDelegate()
        const val MAX_MEMORY_SIZE = 20
    }
}