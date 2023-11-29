package de.konqi.roborockbridge.roborockbridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestBeanProvider {
    val objectMapper = jacksonObjectMapper()

    @Bean
    fun objectMapper(): ObjectMapper {
        return objectMapper
    }
}

