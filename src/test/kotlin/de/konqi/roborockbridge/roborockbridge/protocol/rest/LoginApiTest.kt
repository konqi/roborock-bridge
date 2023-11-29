package de.konqi.roborockbridge.roborockbridge.protocol.rest

import de.konqi.roborockbridge.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockCredentials
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [LoginApi::class, RoborockCredentials::class, RestTemplate::class, TestBeanProvider::class, RoborockCredentials::class])
class LoginApiTest(@Autowired val loginApi: LoginApi, @Autowired val roborockCredentials: RoborockCredentials) {
    @Test
    fun testLogin() {
        val login = loginApi.login()

        roborockCredentials.hmacKey = login.rriot.hmacKey
        roborockCredentials.sessionId = login.rriot.sessionId
        roborockCredentials.restApiRemote = login.rriot.remote.api
        roborockCredentials.restApiToken = login.token
        roborockCredentials.mqttServer = login.rriot.remote.mqttServer
        roborockCredentials.mqttKey = login.rriot.mqttKey
        roborockCredentials.userId = login.rriot.userId
    }
}