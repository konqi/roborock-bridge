package de.konqi.roborockbridge.roborockbridge.protocol

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.login.Rriot
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*

@Component
class RoborockCredentials(@Autowired private val objectMapper: ObjectMapper) {
    @Value("\${username:}")
    @JsonIgnore
    var username: String = ""

    @Value("\${password:}")
    @JsonIgnore
    var password: String = ""

    @Value("\${baseUrl:https://euiot.roborock.com}")
    @JsonIgnore
    var baseUrl = ""

    @Value("\${clientId:}")
    @JsonIgnore
    var clientId = ""

    var userId: String? = null
    var restApiRemote: String? = null
    var restApiToken: String? = null
    var mqttServer: String? = null
    var hmacKey: String? = null
    var mqttKey: String? = null
    var sessionId: String? = null
    var homeId: Int? = null

    @get:JsonIgnore
    val isConfigured: Boolean get() = username.isNotBlank() && password.isNotBlank()

    @get:JsonIgnore
    val isLoggedIn: Boolean get() = !userId.isNullOrBlank()

    @PostConstruct
    fun validate() {
        if (this.username.isBlank()) {
            logger.error("username not set. Please add username property to configuration file.")
        }
        if (this.password.isBlank()) {
            logger.error("password not set. Please add username property to configuration file.")
        }
        if (this.clientId.isBlank()) {
            this.clientId = generateClientId(this.username)
            logger.info("clientId not configured. Using auto-generated clientId ${this.clientId}.")
        }

        try {
            FileInputStream(CREDENTIAL_FILENAME).use { fis ->
                val values = objectMapper.readValue<Map<String, String>>(fis)
                this.restApiRemote = values["restApiRemote"]
                this.restApiToken = values["restApiToken"]
                this.mqttServer = values["mqttServer"]
                this.hmacKey = values["hmacKey"]
                this.mqttKey = values["mqttKey"]
                this.sessionId = values["sessionId"]
                this.userId = values["userId"]
                this.homeId = values["homeId"]?.toInt()
            }
        } catch (e: FileNotFoundException) {
            logger.info("Could not load credentials. No file '${CREDENTIAL_FILENAME}' found.")
        }
    }

    fun fromRriot(rriot: Rriot, restApiToken: String) {
        this.hmacKey = rriot.hmacKey
        this.sessionId = rriot.sessionId
        this.restApiRemote = rriot.remote.api
        this.restApiToken = restApiToken
        this.mqttServer = rriot.remote.mqttServer
        this.mqttKey = rriot.mqttKey
        this.userId = rriot.userId
    }

    @PreDestroy
    fun persist() {
        try {
            FileOutputStream(CREDENTIAL_FILENAME).use { fos -> objectMapper.writeValue(fos, this) }
        } catch (e: Exception) {
            logger.info("Could not save credentials to file '${CREDENTIAL_FILENAME}', ${e.message}.")
        }
    }

    companion object {
        private const val CREDENTIAL_FILENAME = "auth.json"
        private val logger by LoggerDelegate()

        /**
         * Generates a unique client identifier (per user)
         */
        fun generateClientId(username: String): String {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(username.toByteArray())
            // might have to change this if application gets banned for some reason
            md5.update(ProtocolUtils.CLIENT_ID.toByteArray())

            return String(Base64.getEncoder().encode(md5.digest()))
        }
    }
}