package de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.user

import java.time.Instant

data class UserApiResponseDto<T>(
    val api: String?,
    val result: T,
    val status: String, // UNAUTHORIZED, ok
    val success: Boolean, // true
    // msg, code, timestamp are set when call is unauthorized (probably an api gateway)
    val msg: String? = null,
    val code: String? = null,
    val timestamp: Instant? = null
)