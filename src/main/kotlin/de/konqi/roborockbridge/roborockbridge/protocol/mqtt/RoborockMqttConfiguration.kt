package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "roborock-mqtt")
data class RoborockMqttConfiguration(
    val nonceGenerationSalt: String,
    val appSecretSalt: String,
    val endpoint: String
)