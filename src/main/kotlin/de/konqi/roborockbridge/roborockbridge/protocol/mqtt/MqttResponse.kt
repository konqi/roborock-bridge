package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class MqttResponse @JsonCreator constructor(
    @get:JsonProperty("t")
    val timestamp: UInt,
    val dps: Map<String, String>
) {
    constructor() : this(0u, emptyMap()) {}
}