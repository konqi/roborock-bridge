package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.protocol.RoborockRestApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class RoborockAuthTest(@Autowired val roborockRestApi: RoborockRestApi) {
    @Test
    fun testLogin() {
//        roborockRestApi.login()
//        roborockRestApi.getHomeDetail()
//        roborockRestApi.getUserHome()
    }

    @Test
    fun testGenerateClientId() {
        assertDoesNotThrow { RoborockRestApi.generateClientId("username") }
    }
}