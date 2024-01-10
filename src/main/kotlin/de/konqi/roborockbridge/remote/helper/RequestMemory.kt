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
    private val deviceMemory: MutableMap<String, LinkedHashMap<Int, RequestData>> = mutableMapOf()
    val now get() = Date().time

    private fun getDeviceMemory(deviceId: String): LinkedHashMap<Int, RequestData> =
        if (deviceMemory[deviceId] == null) {
            val map: LinkedHashMap<Int, RequestData> = LinkedHashMap(MAX_MEMORY_SIZE)
            deviceMemory[deviceId] = map
            map
        } else {
            deviceMemory[deviceId]!!
        }


    fun put(deviceId: String, key: Int, value: RequestData): RequestData? {
        val memory = getDeviceMemory(deviceId)
        while (memory.size >= MAX_MEMORY_SIZE) {
            val lost = memory.pollLastEntry()
            logger.warn(
                "Request {} with method '{}' evicted after {} ms",
                lost.first,
                lost.second.method,
                now - lost.second.requestTimeMs
            )
        }
        return memory.put(key, value)
    }

    fun remove(deviceId: String, key: Int): RequestData? {
        val memory = getDeviceMemory(deviceId)
        val value = memory.remove(key)

        if (value != null) {
            logger.debug(
                "Request {} with method '{}' finished after {} ms",
                key,
                value.method,
                now - value.requestTimeMs
            )
        }

        return value
    }

    fun get(deviceId: String, key: Int) = getDeviceMemory(deviceId)[key]

//    fun findUnresponsiveDevices(unresponsiveTimeoutMs: Long): Set<String> =
//        deviceMemory.filter { device -> device.value.values.minBy { request -> request.requestTimeMs }.requestTimeMs < (now - unresponsiveTimeoutMs) }.keys

    fun clearMessagesOlderThan(ageMs: Long): List<Pair<String, Int>> =
        deviceMemory.keys.associateWith { deviceId -> deviceMemory[deviceId]?.filter { (_, requestData) -> (requestData.requestTimeMs < (now - ageMs)) }?.keys }
            .flatMap { (deviceId, requestIds) ->
                requestIds?.map { id -> deviceId to id } ?: listOf()
            }.filter { (deviceId, requestId) ->
                deviceMemory[deviceId]?.remove(requestId).let { removedRequest ->
                    if (removedRequest != null) {
                        logger.debug(
                            "Request {} pruned from memory (was age: {} ms)",
                            requestId,
                            now - removedRequest.requestTimeMs
                        )
                        true
                    } else false
                }
            }


    companion object {
        private val logger by LoggerDelegate()
        const val MAX_MEMORY_SIZE = 20
    }
}
