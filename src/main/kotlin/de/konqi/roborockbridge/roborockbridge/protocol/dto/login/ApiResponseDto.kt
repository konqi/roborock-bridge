package de.konqi.roborockbridge.roborockbridge.protocol.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    val data: T? = null,
    val code: Int,
    val msg: String
)