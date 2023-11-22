package de.konqi.roborockbridge.roborockbridge.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.helper.DeviceKeyMemory
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.raw.EncryptedMessage
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request.Protocol101Wrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol102Dps
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MessageDecoder(
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val deviceKeyMemory: DeviceKeyMemory,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val request101Factory: Request101Factory
) {
    fun decode(deviceId: String, payload: ByteArray): Any? {
        val key = deviceKeyMemory[deviceId]
        val data = EncryptedMessage(key!!, payload)
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
        val protocol102Wrapper: de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol102Wrapper = objectMapper.readValue(data.payload)
        if (!protocol102Wrapper.dps.keys.all { it == protocol }) {
            logger.warn("Weird packet $protocol102Wrapper")
            return null
        }
        val body = protocol102Wrapper.dps[protocol]

        return if (body != null) {
            // Reed as generic Json, because we need the request id to match the correct response object
            val protocol102Dps: Protocol102Dps<ArrayNode> = objectMapper.readValue(body)
            // the id matches the request, the request determines the response object
            val requestMethod = requestMemory.getAndDestroy(protocol102Dps.id)

            return if(requestMethod != null) {
                Protocol102Dps(
                    id = protocol102Dps.id,
                    result = objectMapper.treeToValue(protocol102Dps.result, requestMethod.decodesTo.java),
                    method = requestMethod
                )
            } else protocol102Dps
        } else null
    }

    fun readProtocol301Body(data: EncryptedMessage, nonceOverride: ByteArray? = null): ByteArray {
        val mqttResponse = Response301(data.payload)
        // get nonce for id (assuming the id in the response matches the one in the request)
        val nonce = nonceOverride ?: request101Factory.generateNonce(mqttResponse.id)
        val decrypted = mqttResponse.decrypt(nonce)
//        println(decrypted)
        return decrypted
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}