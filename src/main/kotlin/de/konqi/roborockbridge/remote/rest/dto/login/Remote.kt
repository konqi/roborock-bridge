package de.konqi.roborockbridge.remote.rest.dto.login

import com.fasterxml.jackson.annotation.JsonProperty

data class Remote(
    @JsonProperty("a") val api: String,
    @JsonProperty("r") val region: String? = null,
    @JsonProperty("l") val logging: String? = null,
    @JsonProperty("m") val mqttServer: String
)