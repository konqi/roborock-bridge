package de.konqi.roborockbridge.roborockbridge.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.persistence.DeviceRepository
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.raw.EncryptedMessage
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

interface DecodedMqttMessage {
    val messageSchemaType: Int
    val deviceId: String
}

data class MessageWrapper<T>(
    override val deviceId: String,
    override val messageSchemaType: Int,
    val requestId: Int,
    val payload: T
) : DecodedMqttMessage

data class StatusUpdate(
    override val deviceId: String,
    override val messageSchemaType: Int,
    val value: Int
) : DecodedMqttMessage

@Component
class MessageDecoder(
    @Autowired private val requestMemory: RequestMemory,
    @Autowired private val objectMapper: ObjectMapper,
    private val robotRepository: DeviceRepository
) {
    fun decode(deviceId: String, payload: ByteBuffer): List<DecodedMqttMessage> {
        val robot = robotRepository.findById(deviceId).orElseThrow {
            RuntimeException("Robot with deviceId '$deviceId' is unknown.")
        }
        val data = EncryptedMessage(robot.deviceKey, payload)
        if (!data.valid) {
            throw RuntimeException("Checksum mismatch in received message.")
        }

        val protocol = "${data.header.protocol}"
        logger.debug("Protocol ${protocol}, message: ${String(data.payload)}")


        return when (protocol.toInt()) {
            // ability to handle 101 requests is only required to decode mqtt traffic captures
            IpcRequestWrapper.SCHEMA_TYPE -> {
                val message = readProtocol101Body(data)
                listOf(
                    MessageWrapper(
                        deviceId = deviceId,
                        messageSchemaType = IpcRequestWrapper.SCHEMA_TYPE,
                        requestId = message.dps.data.requestId,
                        payload = message
                    )
                )
            }

            IpcResponseWrapper.SCHEMA_TYPE -> {
                val messages = readIpcResponse(data)
                messages.map { (proto, payload) ->
                    if (proto == 102) {
                        payload as IpcResponseDps<*>
                        MessageWrapper(
                            deviceId = deviceId,
                            messageSchemaType = IpcResponseWrapper.SCHEMA_TYPE,
                            requestId = payload.id,
                            payload = payload
                        )
                    } else {
                        payload as Int
                        StatusUpdate(deviceId = deviceId, messageSchemaType = proto, value = payload)
                    }
                }
            }

            Protocol301Wrapper.SCHEMA_TYPE -> {
                val message = readProtocol301Body(data)
                listOf(
                    MessageWrapper(
                        deviceId = deviceId,
                        messageSchemaType = Protocol301Wrapper.SCHEMA_TYPE,
                        requestId = message.id.toInt(),
                        payload = message
                    )
                )
            }

            else -> {
                logger.warn("Unknown protocol $protocol")
                emptyList()
            }
        }
    }

    fun readProtocol101Body(data: EncryptedMessage): IpcRequestWrapper {
        return objectMapper.readValue<IpcRequestWrapper>(data.payload)
    }

    fun readIpcResponse(
        data: EncryptedMessage
    ): Map<Int, Any> {
        val protocol102Wrapper: IpcResponseWrapper = objectMapper.readValue(data.payload)

        return protocol102Wrapper.dps.map { (nestedProtocolIdentifier, value) ->
            if (nestedProtocolIdentifier == "102") {
                // this is an IPC response
                nestedProtocolIdentifier.toInt() to readIpcResponseBody(value)
            } else {
                // this is one or more status updates
                nestedProtocolIdentifier.toInt() to value.toInt()
            }
        }.toMap()
    }

    private fun readIpcResponseBody(body: String?): IpcResponseDps<out Any> {
        return if (!body.isNullOrEmpty()) {
            // Reed as generic Json, because we need the request id to match the correct response object
            val protocol102Dps: IpcResponseDps<JsonNode> = objectMapper.readValue(body)
            // the id matches the request, the request determines the response object
            val requestData = requestMemory[protocol102Dps.id]

            if (requestData != null) {
                // for map requests we get two responses, one with a confirmation of the request (proto 102) and map data (proto 301)
                if (requestData.nonce == null) {
                    logger.info("Received response to request ${protocol102Dps.id} with method '${protocol102Dps.method}'.")
                    requestMemory.remove(protocol102Dps.id)
                } else {
                    logger.info("Request ${protocol102Dps.id} with method '${protocol102Dps.method}' was confirmed via IPS Response.")
                }

                // return fully parsed payload
                IpcResponseDps(
                    id = protocol102Dps.id,
                    result = objectMapper.treeToValue(protocol102Dps.result, requestData.method.decodesTo.java),
                    method = requestData.method,
                )
            } else {
                logger.warn("Received response to unknown request ${protocol102Dps.id} with method '${protocol102Dps.method}'.")

                protocol102Dps
            }
        } else {
            throw RuntimeException("Cannot decode empty body")
        }
    }

    fun readProtocol301Body(data: EncryptedMessage): Protocol301 {
        val mqttResponse = Protocol301Binary.fromRawBytes(data.payload)
        val requestData = requestMemory.getAndRemove(mqttResponse.id.toInt())
        // get nonce for id (assuming the id in the response matches the one in the request)
        return if (requestData?.nonce != null) {
            val decrypted = mqttResponse.decryptAndDecode(requestData.nonce)
            logger.info("Received response to request ${mqttResponse.id} with method '${requestData.method}'.")
            decrypted
        } else {
            throw RuntimeException("Found no nonce in memory to decode. Request did not origin from service or too much backpressure.")
        }
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}