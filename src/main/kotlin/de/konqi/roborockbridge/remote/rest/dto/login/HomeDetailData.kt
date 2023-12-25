package de.konqi.roborockbridge.remote.rest.dto.login

data class HomeDetailData (
    val id: Int,
    val name: String,
    val tuyaHomeId: Int,
    val rrHomeId: Int,
    val deviceListOrder: Int?
)