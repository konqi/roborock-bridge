package de.konqi.roborockbridge.remote.mqtt.ipc.request.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// app_start [{"clean_mop":0}]
@JsonIgnoreProperties(ignoreUnknown = true)
data class AppStartDTO(
    @get:JsonProperty("clean_mop")
    val cleanMop: Int = 0,
) : IpcRequestDTO
