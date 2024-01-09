package de.konqi.roborockbridge.remote.helper

import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.remote.mqtt.RequestMethod
import de.konqi.roborockbridge.utility.pollLastEntry
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
                lost.first,
                lost.second.method,
                Date().time - lost.second.requestTimeMs
            )
        }
        return memory.put(key, value)
    }

    fun remove(key: Int): RequestData? {
        val value = memory.remove(key)

        if (value != null) {
            logger.debug(
                "Request {} with method '{}' finished after {} ms",
                key,
                value.method,
                Date().time - value.requestTimeMs
            )
        }

        return value
    }

    fun get(key: Int) = memory[key]

    companion object {
        private val logger by LoggerDelegate()
        const val MAX_MEMORY_SIZE = 20
    }
}
