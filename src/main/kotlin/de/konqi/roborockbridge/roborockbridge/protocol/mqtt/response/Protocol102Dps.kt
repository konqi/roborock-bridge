package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response

import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.RequestMethod

data class Protocol102Dps<T>(
    val id: Int,
    val result: T,
    var method: RequestMethod? = null
)