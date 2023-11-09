package de.konqi.roborockbridge.roborockbridge.protocol.dto.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Remote(
    @SerialName("a") val api: String,
    @SerialName("r") val region: String? = null,
    @SerialName("l") val l: String? = null,
    @SerialName("m") val mqttServer: String
)