package de.konqi.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.protocol.ProtocolUtils
import de.konqi.roborockbridge.protocol.mqtt.raw.EncryptedMessage
import de.konqi.roborockbridge.protocol.mqtt.ipc.request.IpcRequestDps
import de.konqi.roborockbridge.protocol.mqtt.ipc.request.IpcRequestPayload
import de.konqi.roborockbridge.protocol.mqtt.ipc.request.IpcRequestPayloadSecurity
import de.konqi.roborockbridge.protocol.mqtt.ipc.request.IpcRequestWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

data class Request101(
    val requestId: UShort,
    val message: EncryptedMessage,
)

@Component
@EnableConfigurationProperties(RoborockMqttConfiguration::class)
class Request101Factory(
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val mqttConfig: RoborockMqttConfiguration
) {
    private val requestIdCounter = AtomicInteger()

    /**
     * Generates predictable 16 bytes from requestId as nonce to pass along secure requests
     *
     * @return 16 bytes
     */
    fun generateNonce(requestId: UShort): ByteArray {
        return with(arrayOf(requestId, mqttConfig.nonceGenerationSalt).joinToString(":")) {
            ProtocolUtils.calcMd5(this).copyOfRange(0, 16)
        }
    }

    fun createRequest(
        method: RequestMethod,
        key: String,
        parameters: List<String> = emptyList(),
        secure: Boolean = false
    ): Request101 {
        val message = EncryptedMessage(key)
        val timestamp = ProtocolUtils.getTimeSeconds()
        val requestId = requestIdCounter.incrementAndGet().toUShort()
        val nonce = generateNonce(requestId)
        val data = IpcRequestWrapper(
            dps = IpcRequestDps(
                data = IpcRequestPayload(
                    requestId = requestId.toInt(),
                    method = method.value,
                    parameters = objectMapper.valueToTree(parameters),
                    security = if (secure)
                        IpcRequestPayloadSecurity(
                            endpoint = mqttConfig.endpoint, nonce = String(Hex.encode(nonce))
                        ) else null
                )
            ), timestamp = timestamp
        )

        message.header.protocol = 101u
        message.header.timestamp = timestamp.toUInt()
        message.payload = objectMapper.writeValueAsBytes(data)

        return Request101(requestId, message)
    }
}