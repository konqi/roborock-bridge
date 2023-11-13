package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.roborockbridge.protocol.Utils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

enum class RequestMethodEnum(val value: String) {
    GET_PROP("get_prop"),
    GET_MAP_V1("get_map_v1")
}

@Component
class RequestFactory(@Autowired private val objectMapper: ObjectMapper) {
    fun createRequest(
        method: RequestMethodEnum,
        params: Array<String> = emptyArray(),
        secure: Boolean = false
    ): String {
        val timestamp = Utils.getTimeSeconds()
        val requestId = requestIdCounter.incrementAndGet()
        val body = hashMapOf(
            "id" to requestId,
            "method" to method.value,
            "params" to params
        ).also {
            if (secure) {
                val security = hashMapOf(
                    "endpoint" to endpoint,
                    nonce to String(Hex.encode(nonce))
                )
                it["security"] = security
            }
        }

        return objectMapper.writeValueAsString(
            hashMapOf(
                "t" to timestamp,
                "dps" to hashMapOf(
                    "101" to objectMapper.writeValueAsString(body)
                )
            )
        )
    }

    companion object {
        private val requestIdCounter = AtomicInteger()
        private val endpoint = Utils.generateNonce()
        private val nonce = Random.nextBytes(16)
    }
}