package de.konqi.roborockbridge.protocol.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.protocol.RoborockCredentials
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [HomeApi::class, HomeApiRestTemplate::class, HomeApiTest.Companion.ProvideJackson::class])
class HomeApiTest(@Autowired val homeApi: HomeApi, @Autowired val roborockCredentials: RoborockCredentials) {
    @Test
    fun getHome() {
        val home = homeApi.getHome()
        roborockCredentials.homeId = home.rrHomeId
    }

    companion object {
        @TestConfiguration
        class ProvideJackson {
            val objectMapper = jacksonObjectMapper()

            @Bean
            fun objectMapper(): ObjectMapper {
                return objectMapper
            }

            @Bean
            fun roborockCredentials(): RoborockCredentials {
                return RoborockCredentials(objectMapper)
            }
        }
    }
}