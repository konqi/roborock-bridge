package de.konqi.roborockbridge.roborockbridge.protocol.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class HomeDetailData (
    val id: Int,
    val name: String,
    val tuyaHomeId: Int,
    val rrHomeId: Int,
    val deviceListOrder: Int?
)