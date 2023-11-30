package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request.Protocol101Payload
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request.Protocol101Wrapper
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [Request101Factory::class, Request101FactoryTest.Companion.ProvideJackson::class])
class Request101FactoryTest(
    @Autowired val request101Factory: Request101Factory
) {
    @Test
    fun getPayload() {
        val (_, requestAmessage) = request101Factory.createRequest(key = "secret", method = RequestMethod.GET_PROP, parameters = listOf("get_status"))
        val requestAPayload = String(requestAmessage.payload)

        assertFalse("security" in requestAPayload)

        val jsonObjA = (JSONObject(requestAPayload)["dps"] as JSONObject)["101"] as String

        assertEquals(JSONObject(jsonObjA)["id"], 1)

        // get map request (id should increment, security should be present)
        val (_, requestBmessage) = request101Factory.createRequest(key = "secret", method = RequestMethod.GET_MAP_V1, secure = true)
        val requestBPayload = String(requestBmessage.payload)

        assertTrue("security" in requestBPayload)

        val jsonObjB = (JSONObject(requestBPayload)["dps"] as JSONObject)["101"] as String

        assertEquals(JSONObject(jsonObjB)["id"], 2)
    }

    @Test
    fun testNonceGeneration() {
        val nonce11 = request101Factory.generateNonce(1u)
        val nonce2 = request101Factory.generateNonce(9999u)
        val nonce12 = request101Factory.generateNonce(1u)

        assertEquals(nonce11.size, 16, "must have size of 16")
        assertFalse(nonce11 contentEquals nonce2, "should not collide in close proximity")
        assertTrue(nonce11 contentEquals nonce12, "result must be reproducible")
    }

    @Test
    fun parseRequest() {
        val nestedValue =
            """{"id":413,"method":"get_map_v1","params":{},"security":{"endpoint":"123456789","nonce":"AFFEAFFEAFFEAFFEAFFEAFFEAFFEAFFE"}}"""
        val parsedNestedValue: Protocol101Payload = objectMapper.readValue(nestedValue)
        assertEquals(parsedNestedValue.requestId, 413)

        val data =
            """{"dps":{"101":"{\"id\":413,\"method\":\"get_map_v1\",\"params\":{},\"security\":{\"endpoint\":\"123456789\",\"nonce\":\"AFFEAFFEAFFEAFFEAFFEAFFEAFFEAFFE\"}}"},"t":1700310549}"""
        val parsed: Protocol101Wrapper = objectMapper.readValue(data)

        assertEquals(parsed.dps.data.requestId, 413)

        assertDoesNotThrow {
            objectMapper.writeValueAsString(parsed)
        }
    }

    companion object {
        val objectMapper = jacksonObjectMapper()

        @TestConfiguration
        class ProvideJackson {
            @Bean
            fun objectMapper(): ObjectMapper {
                return objectMapper
            }
        }
    }
}