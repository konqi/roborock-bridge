package de.konqi.roborockbridge.remote.mqtt.ipc.request.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StringDTO(val value: String) : IpcRequestDTO