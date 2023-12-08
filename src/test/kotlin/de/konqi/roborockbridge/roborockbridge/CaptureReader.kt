package de.konqi.roborockbridge.roborockbridge

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.roborockbridge.persistence.DeviceRepository
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Device
import de.konqi.roborockbridge.roborockbridge.protocol.MessageDecoder
import de.konqi.roborockbridge.roborockbridge.protocol.MessageWrapper
import de.konqi.roborockbridge.roborockbridge.protocol.StatusUpdate
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestData
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.RequestMethod
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.GetPropGetStatusResponse
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol301
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol301Wrapper
import de.konqi.roborockbridge.roborockbridge.utility.cast
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.codec.Hex
import org.springframework.test.context.TestPropertySource
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*


@ConfigurationProperties(prefix = "capture-reader")
data class CaptureReaderConfiguration(
    val devices: Map<String, String>,
    val wiresharkExport: String = "simple.json",
    val csvFile: String = "simple.csv"
)

/**
 * This class is used to decode captures of the mqtt communication by the roborock app
 * You have to add the device key of your robot to the configuration file. The structure
 * should be:
 * ```capture-reader.devices[deviceId]=deviceKey```
 *
 * TODO: Make app print device key on first run otherwise find device key via mqtt
 */
@SpringBootTest(classes = [CaptureReader.Companion.ProvideJackson::class, RequestMemory::class, MessageDecoder::class])
@TestPropertySource("classpath:application-dev.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CaptureReader {
    @Autowired
    lateinit var requestMemory: RequestMemory

    @Autowired
    lateinit var messageDecoder: MessageDecoder

    @Autowired
    lateinit var captureReaderConfiguration: CaptureReaderConfiguration

    @Test
    @Order(1)
    @Disabled
    fun readCapture() {
        FileInputStream(captureReaderConfiguration.wiresharkExport).use {
            FileOutputStream(captureReaderConfiguration.csvFile).use { fos ->
                JsonFactory().createParser(it).use { jsonParser ->
                    var topic = ""
                    while (null != jsonParser.nextToken()) {
                        val fieldName = jsonParser.currentName
                        if (fieldName == "mqtt.topic") {
                            jsonParser.nextToken()
                            topic = jsonParser.text
                        } else if (fieldName == "mqtt.msg") {
                            jsonParser.nextToken()
                            val msg = jsonParser.text.split(":").joinToString("")
                            fos.write("$topic:$msg\n".toByteArray())
                        }
                    }
                }
            }
        }
    }

    @Test
    @Order(2)
//    @Disabled
    fun readSimple() {
        FileInputStream(captureReaderConfiguration.csvFile).use { fis ->
            Scanner(fis).use { scanner ->
                while (scanner.hasNextLine()) {
                    try {
                        val line = scanner.nextLine()
                        val (topic, message) = line.split(":")
                        val deviceId = topic.substring(topic.lastIndexOf("/") + 1)
                        val decodedMessages =
                            messageDecoder.decode(deviceId, ByteBuffer.wrap(Hex.decode(message)))
                        decodedMessages.forEach { decodedMessage ->
                            when (decodedMessage.messageSchemaType) {
                                IpcRequestWrapper.SCHEMA_TYPE -> {
                                    val ipcRequest = cast<MessageWrapper<IpcRequestWrapper>>(decodedMessage)

                                    println(
                                        "->  ${ipcRequest.requestId} ${ipcRequest.payload.dps.data.method} ${
                                            objectMapper.writeValueAsString(
                                                ipcRequest.payload.dps.data.parameters
                                            )
                                        }"
                                    )

                                    if (ipcRequest.payload.dps.data.security != null) {
                                        requestMemory[ipcRequest.payload.dps.data.requestId] = RequestData(
                                            RequestMethod.valueOf(ipcRequest.payload.dps.data.method.uppercase()),
                                            Hex.decode(ipcRequest.payload.dps.data.security!!.nonce)
                                        )
                                    } else {
                                        requestMemory[ipcRequest.payload.dps.data.requestId] =
                                            RequestData(RequestMethod.valueOf(ipcRequest.payload.dps.data.method.uppercase()))
                                    }
                                }

                                IpcResponseWrapper.SCHEMA_TYPE -> {
                                    val ipcResponse = cast<MessageWrapper<IpcResponseDps<*>>>(decodedMessage)

                                    if (ipcResponse.payload.result is Array<*> && (ipcResponse.payload.result as Array<*>)[0] is GetPropGetStatusResponse) {
                                        println(
                                            " <- ${ipcResponse.payload.id} ${RequestMethod.GET_PROP.value} ${
                                                objectMapper.writeValueAsString(
                                                    ipcResponse.payload.result
                                                )
                                            }"
                                        )
                                    } else if (ipcResponse.payload.result is ArrayNode || ipcResponse.payload.result is JsonNode) {
                                        println(
                                            " <- ${ipcResponse.payload.id} ${ipcResponse.payload.method?.value} !!arbitrary!! ${
                                                objectMapper.writeValueAsString(
                                                    ipcResponse.payload.result
                                                )
                                            }"
                                        )
                                    } else {
                                        println(" <- ${ipcResponse.payload.id} ${objectMapper.writeValueAsString(ipcResponse.payload.result)}")
                                    }
                                }

                                Protocol301Wrapper.SCHEMA_TYPE -> {
                                    val mapResponse = cast<MessageWrapper<Protocol301>>(decodedMessage)
                                    println(" <- Map Data ${mapResponse.payload.payload.robotPosition?.toString()}")
                                }

                                else -> {
                                    // must be status update
                                    val statusUpdate = decodedMessage as StatusUpdate
                                    println(" <- Status Update for value ${statusUpdate.messageSchemaType} = ${statusUpdate.value}")
                                }
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }

    companion object {
        val objectMapper = jacksonObjectMapper()

        @TestConfiguration
        @EnableConfigurationProperties(CaptureReaderConfiguration::class)
        class ProvideJackson(@Autowired val knownDevices: CaptureReaderConfiguration) {
            @Bean
            fun objectMapper(): ObjectMapper {
                return objectMapper
            }

            @Bean
            fun robotRepository(): DeviceRepository {
                val mock = Mockito.mock(DeviceRepository::class.java)
                Mockito.`when`(mock.findById(Mockito.anyString())).then {
                    val deviceId = it.arguments.first().toString()
                    Optional.of(
                        Device(
                            deviceId = deviceId,
                            deviceKey = knownDevices.devices[deviceId]
                                ?: throw RuntimeException("Must configure device $deviceId in properties"),
                            state = emptyList(),
                            serialNumber = "",
                            model = "",
                            firmwareVersion = "",
                            productName = "",
                            name = "",
                            home = Home(homeId = 0, name = "")

                        )
                    )
                }
                return mock
            }
        }

    }

}