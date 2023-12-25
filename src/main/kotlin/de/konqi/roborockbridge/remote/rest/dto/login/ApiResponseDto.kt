package de.konqi.roborockbridge.remote.rest.dto.login

data class ApiResponseDto<T>(
    val data: T? = null,
    val code: Int,
    val msg: String
)