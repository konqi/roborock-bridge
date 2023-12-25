package de.konqi.roborockbridge.remote.mqtt.ipc.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

data class IpcRequestDps(
    @get:JsonProperty("101")
    @JsonDeserialize(converter = Protocol101PayloadDeserializer::class)
    @JsonSerialize(converter = Protocol101PayloadSerializer::class)
    val data: IpcRequestPayload
)