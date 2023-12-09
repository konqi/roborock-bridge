package de.konqi.roborockbridge.protocol.mqtt.ipc.request

data class IpcRequestPayloadSecurity(val endpoint: String, val nonce: String)