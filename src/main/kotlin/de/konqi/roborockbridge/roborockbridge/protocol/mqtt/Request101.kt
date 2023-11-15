package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.roborockbridge.protocol.Utils
import org.springframework.security.crypto.codec.Hex
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

enum class RequestMethodEnum(val value: String) {
    GET_PROP("get_prop"), GET_MAP_V1("get_map_v1")
}

internal data class Protocol101RequestSecurity(val endpoint: String, val nonce: String)

@JsonPropertyOrder("id", "method", "params", "security")
internal data class Protocol101Request(
    @get:JsonProperty("id")
    val requestId: UInt,
    val method: String,
    @get:JsonProperty("params")
    val parameters: Array<String> = emptyArray(),
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var security: Protocol101RequestSecurity? = null
)

class Request101(key: String, method: RequestMethodEnum, parameters: Array<String> = emptyArray()) :
    EncryptedMessage(key) {
    private val timestamp = Utils.getTimeSeconds()
    private val requestId = requestIdCounter.incrementAndGet()

    private val body = Protocol101Request(
        requestId = requestId.toUInt(),
        method = method.value,
        parameters = parameters
    )

    constructor(
        key: String,
        method: RequestMethodEnum,
        parameters: Array<String> = emptyArray(),
        secure: Boolean
    ) : this(key, method, parameters) {
        if (secure) {
            body.security = Protocol101RequestSecurity(
                endpoint = endpoint, nonce = String(Hex.encode(nonce))
            )
        }

        super.payload = producePayload()
    }

    init {
        super.header.protocol = 101u
        super.header.timestamp = timestamp.toUInt()
        super.payload = producePayload()
    }

    private fun producePayload() =
        createPayload(timestamp = timestamp, protocol = "101", body = objectMapper.writeValueAsString(body)).let {
            objectMapper.writeValueAsString(it)
        }.toByteArray()

    companion object {
        private val requestIdCounter = AtomicInteger()

        // @Todo Should be static / deterministic
        private val endpoint = Utils.generateNonce()
        private val nonce = Random.nextBytes(16)

        // @Todo not very pretty to create a separate instance of object mapper
        private val objectMapper = jacksonObjectMapper()

        private fun createPayload(timestamp: Long, protocol: String, body: String) = hashMapOf(
            "t" to timestamp, "dps" to hashMapOf(
                protocol to body
            )
        )
    }
}