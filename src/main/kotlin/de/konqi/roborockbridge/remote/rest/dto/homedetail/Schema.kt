package de.konqi.roborockbridge.remote.rest.dto.homedetail

data class Schema(
    val id: String,
    val name: String,
    val code: String,
    val mode: String,
    val type: String,
    val property: String?,
    val desc: String?,
)