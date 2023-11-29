package de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.login

data class AuthenticationResponseData(
    val uid: Int? = null,
    val country: String? = null,
    val tuyaDeviceState: Int? = null,
    val rriot: Rriot,
    val avatarurl: String? = null,
    val countrycode: String? = null,
    val nickname: String? = null,
    val rruid: String? = null,
    val region: String? = null,
    val tokentype: String? = null,
    val token: String
)