package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserHomeDto(
    val api: String,
    val result: UserHomeData,
    val status: String, // UNAUTHORIZED, ok
    val success: Boolean, // true
    // msg, code, timestamp are set when call is unauthorized (probably an api gateway)
    val msg: String? = null,
    val code: String? = null,
    val timestamp: Instant? = null
)