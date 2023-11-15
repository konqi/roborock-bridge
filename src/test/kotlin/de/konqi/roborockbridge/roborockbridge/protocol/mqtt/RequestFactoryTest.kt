package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

class RequestFactoryTest() {
    @Test
    fun getPayload() {
        val requestA = Request101(key = "secret", method = RequestMethodEnum.GET_PROP, parameters = arrayOf("get_status"))
        val requestAPayload = String(requestA.payload)
        assertFalse("security" in requestAPayload)
        val jsonObjA = (JSONObject(requestAPayload)["dps"] as JSONObject)["101"] as String
        assertEquals(JSONObject(jsonObjA)["id"], 1)

        // get map request (id should increment, security should be present)
        val requestB = Request101(key = "secret", method = RequestMethodEnum.GET_MAP_V1, secure = true)
        val requestBPayload = String(requestB.payload)
        assertTrue("security" in requestBPayload)
        val jsonObjB = (JSONObject(requestBPayload)["dps"] as JSONObject)["101"] as String
        assertEquals(JSONObject(jsonObjB)["id"], 2)
    }

    companion object {
        @TestConfiguration
        class ProvideJackson {
            @Bean
            fun objectMapper(): ObjectMapper {
                return jacksonObjectMapper()
            }
        }
    }
}