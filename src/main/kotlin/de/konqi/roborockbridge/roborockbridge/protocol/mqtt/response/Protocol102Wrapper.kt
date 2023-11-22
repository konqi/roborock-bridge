package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Protocol102Wrapper @JsonCreator constructor(
    @get:JsonProperty("t")
    val timestamp: UInt,
    val dps: Map<String, String>
) {
    constructor() : this(0u, emptyMap()) {}
}