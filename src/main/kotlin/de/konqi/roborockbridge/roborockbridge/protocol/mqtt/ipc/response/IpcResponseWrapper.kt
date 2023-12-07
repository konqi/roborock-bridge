package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class IpcResponseWrapper @JsonCreator constructor(
    @get:JsonProperty("t")
    val timestamp: UInt,
    val dps: Map<String, String>
) {
    constructor() : this(0u, emptyMap()) {}

    companion object {
        const val SCHEMA_TYPE = 102
    }

}