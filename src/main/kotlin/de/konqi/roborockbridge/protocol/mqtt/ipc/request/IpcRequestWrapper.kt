package de.konqi.roborockbridge.protocol.mqtt.ipc.request

import com.fasterxml.jackson.annotation.JsonProperty

data class IpcRequestWrapper(
    val dps: IpcRequestDps,
    @get:JsonProperty("t")
    val timestamp: Long
) {
    companion object {
        const val SCHEMA_TYPE = 101
    }
}