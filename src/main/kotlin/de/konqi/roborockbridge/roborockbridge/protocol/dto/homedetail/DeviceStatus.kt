package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatus(
    @SerialName("120")
    val n120: Long,
    @SerialName("121")
    val n121: Long,
    @SerialName("122")
    val n122: Long,
    @SerialName("123")
    val n123: Long,
    @SerialName("124")
    val n124: Long,
    @SerialName("125")
    val n125: Long,
    @SerialName("126")
    val n126: Long,
    @SerialName("127")
    val n127: Long,
    @SerialName("128")
    val n128: Long,
    @SerialName("133")
    val n133: Long,
    @SerialName("134")
    val n134: Long,
)