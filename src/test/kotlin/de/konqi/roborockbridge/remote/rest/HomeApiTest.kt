package de.konqi.roborockbridge.remote.rest

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.RoborockCredentials
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [HomeApi::class, HomeApiRestTemplate::class, HomeApiTest.Companion.ProvideJackson::class, TestBeanProvider::class])
class HomeApiTest(@Autowired val homeApi: HomeApi, @Autowired val roborockCredentials: RoborockCredentials) {
    @Test
    fun getHome() {
        val home = homeApi.getHome()
        roborockCredentials.homeId = home.rrHomeId
    }

    companion object {
        @TestConfiguration
        class ProvideJackson {
            @Bean
            fun roborockCredentials(@Autowired objectMapper: ObjectMapper): RoborockCredentials {
                return RoborockCredentials(objectMapper)
            }
        }
    }
}