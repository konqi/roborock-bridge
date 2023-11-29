package de.konqi.roborockbridge.roborockbridge.protocol.rest

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockCredentials
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.login.ApiResponseDto
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.login.AuthenticationResponseData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

class LoginApi(@Autowired val credentials: RoborockCredentials, @Autowired private val restTemplate: RestTemplate) {
    fun login(): AuthenticationResponseData {
        val request = HttpEntity(HttpEntity.EMPTY, HttpHeaders().apply {
            set("header_clientid", credentials.clientId)
        })
        val uri = UriComponentsBuilder.fromHttpUrl("${credentials.baseUrl}${LOGIN_PATH}")
            .queryParam("username", "{username}")
            .queryParam("password", "{password}")
            .queryParam("needtwostepauth", "false")
            .encode()
            .toUriString()

        val response = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            request,
            object : ParameterizedTypeReference<ApiResponseDto<AuthenticationResponseData>>() {},
            mapOf(
                "username" to credentials.username,
                "password" to credentials.password
            )
        )
        val json = response.body

        logger.debug("login call response body return code: ${json?.code}")
        when (json?.code) {
            200 -> { /* OK */
            }

            2008 -> throw UsernameNotFoundException("Unknown user")
            2012 -> throw BadCredentialsException("incorrect password")
            // add more here?
            else -> throw UnknownError("Unknown return code")
        }

        return if (json.data === null) {
            throw AuthenticationServiceException("api did not return authentication data")
        } else json.data

    }

    companion object {
        private val logger by LoggerDelegate()

        const val LOGIN_PATH = "/api/v1/login"
    }
}