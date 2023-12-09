package de.konqi.roborockbridge.protocol.mqtt.ipc.response

import com.fasterxml.jackson.annotation.JsonIgnore
import de.konqi.roborockbridge.protocol.mqtt.RequestMethod

data class IpcResponseDps<T>(
    val id: Int,
    val result: T,
    @JsonIgnore
    var method: RequestMethod? = null,
)