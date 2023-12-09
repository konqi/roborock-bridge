package de.konqi.roborockbridge.protocol.rest.dto.login

data class ApiResponseDto<T>(
    val data: T? = null,
    val code: Int,
    val msg: String
)