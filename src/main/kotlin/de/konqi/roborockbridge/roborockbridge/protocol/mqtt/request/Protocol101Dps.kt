package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

data class Protocol101Dps(
    @get:JsonProperty("101")
    @JsonDeserialize(converter = Protocol101PayloadDeserializer::class)
    @JsonSerialize(converter = Protocol101PayloadSerializer::class)
    val data: Protocol101Payload
)