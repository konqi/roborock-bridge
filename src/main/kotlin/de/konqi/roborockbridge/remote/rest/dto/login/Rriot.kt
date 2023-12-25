package de.konqi.roborockbridge.remote.rest.dto.login

import com.fasterxml.jackson.annotation.JsonProperty

data class Rriot(
    @JsonProperty("r") var remote: Remote,
    // session (just a guess)
    @JsonProperty("s") var sessionId: String,
    // userid
    @JsonProperty("u") var userId: String,
    // used for api calls
    @JsonProperty("h") var hmacKey: String,
    // used for mqtt connection
    @JsonProperty("k") var mqttKey: String? = null
)