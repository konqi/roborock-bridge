package de.konqi.roborockbridge.remote.mqtt.ipc.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.JsonNode
import de.konqi.roborockbridge.utility.NestedJsonDeserializer
import de.konqi.roborockbridge.utility.NestedJsonSerializer

@JsonPropertyOrder("id", "method", "params", "security")
data class IpcRequestPayload(
    @get:JsonProperty("id")
    val requestId: Int, // UShort
    val method: String,
    @get:JsonProperty("params")
    val parameters: JsonNode,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var security: IpcRequestPayloadSecurity? = null
)

internal class Protocol101PayloadSerializer : NestedJsonSerializer<IpcRequestPayload>(IpcRequestPayload::class.java)
internal class Protocol101PayloadDeserializer : NestedJsonDeserializer<IpcRequestPayload>(IpcRequestPayload::class.java)
