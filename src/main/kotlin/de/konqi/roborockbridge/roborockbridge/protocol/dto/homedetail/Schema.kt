package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

data class Schema(
    val id: String,
    val name: String,
    val code: String,
    val mode: String,
    val type: String,
    val property: String?,
    val desc: Any?,
)