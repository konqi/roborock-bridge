package de.konqi.roborockbridge.roborockbridge.protocol.helper

import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.RequestMethod
import org.springframework.stereotype.Component

data class RequestData(val method:RequestMethod, val nonce: ByteArray? = null)

@Component
class RequestMemory :  LinkedHashMap<Int, RequestData>(MAX_MEMORY_SIZE) {

    override fun put(key: Int, value: RequestData): RequestData? {
        if (size >= MAX_MEMORY_SIZE) {
            pollLastEntry()
        }
        return super.put(key, value)
    }

    fun getAndRemove(key: Int): RequestData? {
        val value = super.get(key)
        if (value != null) {
            super.remove(key)
        }

        return value
    }

    companion object {
        const val MAX_MEMORY_SIZE = 20
    }
}