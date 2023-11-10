package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Device
import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Product
import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Room
import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.UserHomeDto
import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.ApiResponseDto
import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.AuthenticationResponseData
import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.HomeDetailData
import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.Rriot
import de.konqi.roborockbridge.roborockbridge.state.Home
import de.konqi.roborockbridge.roborockbridge.state.Robot
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.RuntimeException

@OptIn(ExperimentalSerializationApi::class)
@Component
class RoborockRestApi(@Autowired val credentials: RoborockCredentials) {
    val clientId = generateClientId(credentials.username)
    private var authentication: AuthenticationResponseData? = load(AUTH_FILENAME)
    private var homeDetail: HomeDetailData? = load(HOME_DETAILS_FILENAME)

    val isLoggedIn: Boolean get() = authentication !== null && homeDetail !== null
    val canLogIn: Boolean get() = credentials.isValid
    val rriot: Rriot get() = if(isLoggedIn) authentication!!.rriot else throw RuntimeException("must login first")

    fun login() {
        val urlSearchParms = mapOf(
            "username" to credentials.username,
            "password" to credentials.password,
            "needtwostepauth" to "false"
        )
        val headers = mapOf(
            "header_clientid" to clientId
        )

        val response =
            khttp.post(url = "$BASE_URL$LOGIN_PATH", headers = headers, params = urlSearchParms, stream = true)
        val json = Json.decodeFromStream<ApiResponseDto<AuthenticationResponseData>>(response.raw)


        logger.debug("login call response body return code: ${json.code}")
        when (json.code) {
            200 -> { /* OK */
            }

            2008 -> throw UsernameNotFoundException("Unknown user")
            2012 -> throw BadCredentialsException("incorrect password")
            // add more here?
            else -> throw UnknownError("Unknown return code")
        }

        if (json.data === null) {
            throw AuthenticationServiceException("api did not return authentication data")
        }

        this.authentication = json.data
        persist(this.authentication, AUTH_FILENAME)
    }

    fun getHomeDetail() {
        val token = this.authentication?.token ?: throw RuntimeException("must login first")

        val response = khttp.get(
            url = "$BASE_URL$GET_HOME_DETAIL_PATH",
            headers = mapOf(
                "Authorization" to token
            ), stream = true
        )

        val json = Json.decodeFromStream<ApiResponseDto<HomeDetailData>>(response.raw)

        logger.debug("getHomeDetail response body return code: ${json.code}")
        when (json.code) {
            200 -> { /* OK */
            }
            // add more here?
            else -> throw UnknownError("Unknown return code")
        }

        this.homeDetail = json.data
        persist(this.homeDetail, HOME_DETAILS_FILENAME)
    }

    fun getUserHome(): Pair<Home, List<Robot>> {
        val homeId = this.homeDetail?.rrHomeId ?: throw RuntimeException("must fetch home detail first")
        val rriot = this.authentication?.rriot ?: throw RuntimeException("must login first")

        var home: Home? = load(HOME_FILENAME)
        var robots: List<Robot>? = load(ROBOTS_FILENAME)

        // @TODO Temporary solution while developing (in production this can be fetched on each start)
        if(home !== null && robots !== null) {
            return (home to robots)
        }

        val baseUrl = rriot.remote.api
        val path = GET_USER_HOME_PATH.replace("{homeId}", homeId.toString())
        val url = "$baseUrl${path}"
        logger.trace("getUserHome url built via aut rriot params: $url")

        val authHeader = getAuthHeader(rriot, path)

        logger.trace("AuthHeader used for getUserHome call: $authHeader")

        val response = khttp.get(
            url = url, headers = mapOf(
                "Authorization" to authHeader
            ),
            stream = true
        )

        val json = Json.decodeFromStream<UserHomeDto>(response.raw)

        logger.debug("getUserHome response body status: ${json.status}")

        val result = json.result

        logger.info("${json.status} ${json.success}")
        if (json.status == "ok" && json.success) {
            home = Home(rooms = result.rooms)
            robots = getRobots(devices = result.devices, products = result.products)

            persist(home, HOME_FILENAME)
            persist(robots, ROBOTS_FILENAME)

            return home to robots
        } else {
            throw RuntimeException("getUserHome returned with error '${json.status}', message (if any): '${json.msg}'")
        }
    }

    companion object {
        private val logger by LoggerDelegate()

        const val AUTH_FILENAME = "auth.json"
        const val HOME_DETAILS_FILENAME = "home-detail.json"
        const val HOME_FILENAME = "user-home.json"
        const val ROBOTS_FILENAME = "robots.json"

        const val BASE_URL = "https://euiot.roborock.com"
        const val LOGIN_PATH = "/api/v1/login"
        const val GET_HOME_DETAIL_PATH = "/api/v1/getHomeDetail"
        const val GET_USER_HOME_PATH = "/user/homes/{homeId}"

        /**
         * Generates a unique client identifier (per user)
         */
        fun generateClientId(username: String): String {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(username.toByteArray())
            // might have to change this if application gets banned for some reason
            md5.update(Utils.CLIENT_ID.toByteArray())

            return String(Base64.getEncoder().encode(md5.digest()))
        }

        private fun calcBase64Hmac(key: String, value: String): String {
            val macEncoder = Mac.getInstance("HmacSHA256")
            val hmacKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
            macEncoder.init(hmacKey)
            macEncoder.update(value.toByteArray())
            val signature = macEncoder.doFinal()

            return String(
                Base64.getEncoder().encode(signature)
            )
        }

        /**
         * Generates a string to be used as the Authentication header in request to the api
         * @param rriot communication parameters retrievable via the [login] method
         * @param path pathname (starting with /) used for the request
         * @return authentication header value
         */
        private fun getAuthHeader(
            rriot: Rriot,
            path: String /*, queryParams: String = "", body: String = ""*/
        ): String {
            logger.debug("creating auth header for request path '$path'")
            val pathnameHash = Utils.calcHexMd5(path)
            // placeholder for future use
            val queryParamsHash = ""
            // placeholder for future use
            val bodyHash = ""
            val nonce = Utils.generateNonce()
            val timestamp = Utils.getTimeSeconds()

            val signature = arrayOf(
                rriot.userId,
                rriot.sessionId,
                nonce,
                timestamp,
                pathnameHash,
                queryParamsHash,
                bodyHash
            ).joinToString(":")

            logger.debug("Signature used to generate authentication header: '$signature'")

            val mac = calcBase64Hmac(key = rriot.hmacKey, value = signature)

            val token = arrayOf(
                """id="${rriot.userId}"""",
                """s="${rriot.sessionId}"""",
                """ts="$timestamp"""",
                """nonce="$nonce"""",
                """mac="$mac""""
            ).joinToString(", ")

            logger.debug("Authentication header value: $token")

            return "Hawk $token"
        }

        fun getRobots(devices: List<Device>, products: List<Product>): List<Robot> {
            return devices.map { device ->
                Robot(
                    deviceId = device.duid,
                    deviceName = device.name,
                    deviceInformation = device,
                    productInformation = products.find { product -> product.id == device.productId }
                        ?: throw RuntimeException("Unable to resolve product information for product id '${device.productId}'"))
            }
        }


        inline fun <reified T> persist(data: T, filename: String) {
            val fos = FileOutputStream(filename)
            Json.encodeToStream(data, fos)
        }

        private inline fun <reified T> load(filename: String): T? {
            return try {
                val fis = FileInputStream(filename)
                Json.decodeFromStream<T>(fis)
            } catch(e: FileNotFoundException) {
                logger.info("No file '$filename' found.")
                null
            }
        }
    }
}