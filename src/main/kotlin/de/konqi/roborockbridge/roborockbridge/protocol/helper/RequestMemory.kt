package de.konqi.roborockbridge.roborockbridge.protocol.helper

import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.RequestMethod
import org.springframework.stereotype.Component

@Component
class RequestMemory : LinkedHashMap<Int, RequestMethod>(MAX_MEMORY_SIZE) {

    override fun put(key: Int, value: RequestMethod): RequestMethod? {
        if (size >= MAX_MEMORY_SIZE) {
            pollLastEntry()
        }
        return super.put(key, value)
    }

    fun getAndDestroy(key: Int): RequestMethod? {
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