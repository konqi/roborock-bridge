package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request

import com.fasterxml.jackson.annotation.JsonProperty

data class Protocol101Wrapper(
    val dps: Protocol101Dps,
    @get:JsonProperty("t")
    val timestamp: Long
)