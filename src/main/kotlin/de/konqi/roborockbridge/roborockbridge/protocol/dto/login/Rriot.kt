package de.konqi.roborockbridge.roborockbridge.protocol.dto.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rriot(
    @SerialName("r") var remote: Remote,
    // session (just a guess)
    @SerialName("s") var sessionId: String,
    // userid
    @SerialName("u") var userId: String,
    // used for api calls
    @SerialName("h") var hmacKey: String,
    // used for mqtt connection
    @SerialName("k") var mqttKey: String? = null
)