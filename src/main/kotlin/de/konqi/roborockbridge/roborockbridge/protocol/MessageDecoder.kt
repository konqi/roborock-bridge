package de.konqi.roborockbridge.roborockbridge.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.persistence.RobotRepository
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.raw.EncryptedMessage
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request.Protocol101Wrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol102Dps
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

@Component
class MessageDecoder(
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val objectMapper: ObjectMapper,
    private val robotRepository: RobotRepository
) {
    fun decode(deviceId: String, payload: ByteBuffer): Any? {
        val robot = robotRepository.getByDeviceId(deviceId).orElseThrow {
            RuntimeException("Robot with deviceId '$deviceId' is unknown.")
        }
        val data = EncryptedMessage(robot.deviceKey, payload)
        if (!data.valid) {
            throw RuntimeException("Checksum mismatch in received message.")
        }

        val protocol = "${data.header.protocol}"
        logger.debug("Protocol ${protocol}, message: ${String(data.payload)}")

        return when (protocol) {
            "101" -> readProtocol101Body(data)
            "102" -> readProtocol102Body(data)
            "301" -> readProtocol301Body(data)
            else -> {
                logger.warn("Unknown protocol $protocol")
            }
        }
    }

    fun readProtocol101Body(data: EncryptedMessage): Protocol101Wrapper {
        return objectMapper.readValue<Protocol101Wrapper>(data.payload)
    }

    fun readProtocol102Body(
        data: EncryptedMessage
    ): Any? {
        val protocol = "${data.header.protocol}"
        val protocol102Wrapper: de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol102Wrapper =
            objectMapper.readValue(data.payload)
        if (!protocol102Wrapper.dps.keys.all { it == protocol }) {
            logger.warn("Weird packet {\"dps\": ${objectMapper.writeValueAsString(protocol102Wrapper.dps)}}")
            return null
        }
        val body = protocol102Wrapper.dps[protocol]

        return if (body != null) {
            // Reed as generic Json, because we need the request id to match the correct response object
            val protocol102Dps: Protocol102Dps<JsonNode> = objectMapper.readValue(body)
            // the id matches the request, the request determines the response object
            val requestData = requestMemory[protocol102Dps.id]

            return if (requestData != null) {
                if (requestData.nonce == null) {
                    // for map requests we get two responses, one with a confirmation of the request (proto 102) and map data (proto 301)
                    requestMemory.remove(protocol102Dps.id)
                }

                Protocol102Dps(
                    id = protocol102Dps.id,
                    result = objectMapper.treeToValue(protocol102Dps.result, requestData.method.decodesTo.java),
                    method = requestData.method
                )
            } else protocol102Dps
        } else null
    }

    fun readProtocol301Body(data: EncryptedMessage): Response301? {
        val mqttResponse = Response301Parser(data.payload)
        val requestData = requestMemory.getAndDestroy(mqttResponse.id.toInt())
        // get nonce for id (assuming the id in the response matches the one in the request)
        return if (requestData?.nonce != null) {
            val decrypted = mqttResponse.decrypt(requestData.nonce)
            val data = mqttResponse.decode(decrypted)
            data
        } else null
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}