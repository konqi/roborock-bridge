package de.konqi.roborockbridge.remote.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "roborock-mqtt")
data class RoborockMqttConfiguration(
    val nonceGenerationSalt: String,
    /**
     * hardcoded in librrcodec.so, encrypted by the value of "com.roborock.iotsdk.appsecret"
     * @see <a href="https://gist.github.com/rovo89/dff47ed19fca0dfdda77503e66c2b7c7">revo89's gist</a>
     */
    val appSecretSalt: String,
    val endpoint: String
)