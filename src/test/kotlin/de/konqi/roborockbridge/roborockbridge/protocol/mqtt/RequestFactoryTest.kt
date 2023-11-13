package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [RequestFactory::class, ObjectMapper::class])
class RequestFactoryTest(@Autowired val requestFactory: RequestFactory) {
    @Test
    fun getPayload() {
        val requestA = requestFactory.createRequest(method = RequestMethodEnum.GET_PROP, params = arrayOf("get_status"))
        assertFalse("security" in requestA)
        val jsonObjA = (JSONObject(requestA)["dps"] as JSONObject)["101"] as String
        assertEquals(JSONObject(jsonObjA)["id"], 1)

        // get map request (id should increment, security should be present)
        val requestB = requestFactory.createRequest(method = RequestMethodEnum.GET_MAP_V1, secure = true)
        assertTrue("security" in requestB)
        val jsonObjB = (JSONObject(requestB)["dps"] as JSONObject)["101"] as String
        assertEquals(JSONObject(jsonObjB)["id"], 2)
    }
}