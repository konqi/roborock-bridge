package de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail

import com.fasterxml.jackson.annotation.JsonProperty

data class DeviceStatus(
    @JsonProperty("120")
    val n120: Long,
    @JsonProperty("121")
    val n121: Long,
    @JsonProperty("122")
    val n122: Long,
    @JsonProperty("123")
    val n123: Long,
    @JsonProperty("124")
    val n124: Long,
    @JsonProperty("125")
    val n125: Long,
    @JsonProperty("126")
    val n126: Long,
    @JsonProperty("127")
    val n127: Long,
    @JsonProperty("128")
    val n128: Long,
    @JsonProperty("133")
    val n133: Long,
    @JsonProperty("134")
    val n134: Long,
)