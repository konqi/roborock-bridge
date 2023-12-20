package de.konqi.roborockbridge.protocol.rest

import de.konqi.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.protocol.ProtocolUtils
import de.konqi.roborockbridge.protocol.RoborockCredentials
import de.konqi.roborockbridge.protocol.rest.dto.user.UserApiResponseDto
import de.konqi.roborockbridge.protocol.rest.dto.user.UserHomeData
import de.konqi.roborockbridge.protocol.rest.dto.user.UserSchema
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Profile("bridge")
class UserApiRestTemplate(@Autowired private val credentials: RoborockCredentials) : RestTemplate() {
    // TODO This will not work - roborockData is late init
    private val hmacEncoder
        get() = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(credentials.hmacKey?.toByteArray(), "HmacSHA256"))
        }

    private val base64Encoder = Base64.getEncoder()

    private fun hMacEncode(value: String): String {
        return String(base64Encoder.encode(hmacEncoder.doFinal(value.toByteArray())))
    }

    private fun getAuthHeader(path: String): String {
        val pathnameHash = ProtocolUtils.calcHexMd5(path)
        // placeholder for future use
        val queryParamsHash = ""
        // placeholder for future use
        val bodyHash = ""
        val nonce = ProtocolUtils.generateNonce()
        val timestamp = ProtocolUtils.getTimeSeconds()

        val signature = arrayOf(
            credentials.userId, credentials.sessionId, nonce, timestamp, pathnameHash, queryParamsHash, bodyHash
        ).joinToString(":")

        val mac = hMacEncode(value = signature)

        val token = arrayOf(
            "id" to credentials.userId, "s" to credentials.sessionId, "ts" to timestamp, "nonce" to nonce, "mac" to mac
        ).joinToString(", ") { """"${it.first}"="${it.second}"""" }

        return "Hawk $token"
    }

    @PostConstruct
    private fun setup() {
        val authenticationHeaderInterceptor = ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.add("Authorization", getAuthHeader(request.uri.path))
            execution.execute(request, body)
        }

        this.setInterceptors(listOf(authenticationHeaderInterceptor))
    }
}

@Component
class UserApi(
    @Autowired private val credentials: RoborockCredentials, @Autowired private val restTemplate: UserApiRestTemplate
) {
    fun getUserHome(homeId: Int): UserHomeData {
        val response = restTemplate.exchange(
            "${credentials.restApiRemote}${GET_HOMES_PATH}",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<UserApiResponseDto<UserHomeData>>() {}, mapOf(
                "homeId" to homeId
            )
        )
        val json = response.body!!

        logger.debug("getUserHome response body status: ${json.status}")

        logger.info("${json.status} ${json.success}")
        if (json.status == "ok" && json.success) {
            return json.result
        } else {
            throw RuntimeException("getUserHome returned with error '${json.status}', message (if any): '${json.msg}'")
        }
    }

    fun getCleanupSchemas(homeId: Int): List<UserSchema> {
        val response = restTemplate.exchange(
            "${credentials.restApiRemote}${GET_SCENES_PATH}",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<UserApiResponseDto<List<UserSchema>>>() {},
            mapOf(
                "homeId" to homeId
            )
        )

        val json = response.body!!

        if (json.status == "ok" && json.success) {
            return json.result
        } else {
            throw RuntimeException("getCleanupSchemas returned with error '${json.status}', message (if any): '${json.msg}'")
        }
    }

    fun startCleanupSchema(sceneId: Int) {
        val response = restTemplate.exchange(
            "${credentials.restApiRemote}${POST_SCENE_EXECUTE_PATH}",
            HttpMethod.POST,
            null,
            object : ParameterizedTypeReference<UserApiResponseDto<List<UserSchema>>>() {},
            mapOf(
                "sceneId" to sceneId
            )
        )
    }


//    const val FOOBAR = "/v2/user/homes/{homeId}"
//    const val BAR = "/user/scene/home/{homeId}"
//    const val FOO = "/user/scene/order?homeId={homeId}"

    companion object {
        private val logger by LoggerDelegate()

        const val GET_HOMES_PATH = "/user/homes/{homeId}"
        const val GET_SCENES_PATH = "/user/scene/home/{homeId}"
        const val POST_SCENE_EXECUTE_PATH = "/user/scene/{sceneId}/execute"
    }
}