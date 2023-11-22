package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.JsonNode
import de.konqi.roborockbridge.roborockbridge.utility.NestedJsonDeserializer
import de.konqi.roborockbridge.roborockbridge.utility.NestedJsonSerializer

@JsonPropertyOrder("id", "method", "params", "security")
data class Protocol101Payload(
    @get:JsonProperty("id")
    val requestId: Int, // UShort
    val method: String,
    @get:JsonProperty("params")
    val parameters: JsonNode,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var security: Protocol101PayloadSecurity? = null
)

internal class Protocol101PayloadSerializer : NestedJsonSerializer<Protocol101Payload>(Protocol101Payload::class.java)
internal class Protocol101PayloadDeserializer : NestedJsonDeserializer<Protocol101Payload>(Protocol101Payload::class.java)
