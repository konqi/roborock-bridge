package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import java.util.Date

data class UserHomeDto(
    val api: String,
    val result: UserHomeData,
    val status: String, // UNAUTHORIZED, ok
    val success: Boolean, // true
    // msg, code, timestamp are set when call is unauthorized (probably an api gateway)
    val msg: String?,
    val code: String?,
    val timestamp: Date?
)