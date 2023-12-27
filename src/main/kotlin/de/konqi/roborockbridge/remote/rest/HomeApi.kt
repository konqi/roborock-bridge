package de.konqi.roborockbridge.remote.rest

import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.remote.RoborockCredentials
import de.konqi.roborockbridge.remote.rest.dto.login.ApiResponseDto
import de.konqi.roborockbridge.remote.rest.dto.login.HomeDetailData
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class HomeApiRestTemplate(@Autowired private val credentials: RoborockCredentials) : RestTemplate() {
    @PostConstruct
    private fun setup() {
        val authenticationHeaderInterceptor =
            ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.add("Authorization", credentials.restApiToken)
                execution.execute(request, body)
            }

        this.setInterceptors(listOf(authenticationHeaderInterceptor))
    }
}

@Component
class HomeApi(
    @Autowired private val credentials: RoborockCredentials,
    @Autowired private val restTemplate: HomeApiRestTemplate
) {
    fun getHome(): HomeDetailData {
        val response = restTemplate.exchange(
            "${credentials.baseUrl}${GET_HOME_DETAIL_PATH}",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<ApiResponseDto<HomeDetailData>>() {})

        val json = response.body

        logger.debug("getHomeDetail response body return code: ${json?.code}")
        when (json?.code) {
            200 -> { /* OK */
            }
            // add more here?
            else -> throw UnknownError("Unknown return code")
        }
        return json.data ?: throw RuntimeException("home detail data missing from response")
    }

    companion object {
        private val logger by LoggerDelegate()

        const val GET_HOME_DETAIL_PATH = "/api/v1/getHomeDetail"
    }
}