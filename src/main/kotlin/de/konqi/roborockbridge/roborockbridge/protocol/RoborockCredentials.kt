package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RoborockCredentials : InitializingBean{
    @Value("\${username:}")
    var username: String = ""

    @Value("\${password:}")
    var password: String = ""

    val isValid: Boolean get() = username.isNotBlank() && password.isNotBlank()

    override fun afterPropertiesSet() {
        if(this.username.isBlank()) {
            logger.error("username not set. Please add username property to configuration file.")
        }
        if(this.password.isBlank()) {
            logger.error("password not set. Please add username property to configuration file.")
        }
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}