package de.konqi.roborockbridge.remote.rest

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.RoborockCredentials
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [UserApi::class, UserApiRestTemplate::class, RoborockCredentials::class, TestBeanProvider::class])
class UserApiTest(@Autowired val userApi: UserApi, @Autowired val roborockCredentials: RoborockCredentials) {
    @Test
    @Disabled
    fun testGetUserHome() {
        assertDoesNotThrow {
            userApi.getUserHome(roborockCredentials.homeId!!)
        }
    }

    @Test
    @Disabled
    fun testGetCleanupSchemas() {
        assertDoesNotThrow {
            val schemas = userApi.getCleanupSchemas(roborockCredentials.homeId!!)
            println(schemas.map { it.id to it.name })
        }
    }

    @Test
    @Disabled
    fun testStartCleanup() {
        userApi.startCleanupSchema(611409)
    }
}