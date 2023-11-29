package de.konqi.roborockbridge.roborockbridge

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.roborockbridge.protocol.MessageDecoder
import de.konqi.roborockbridge.roborockbridge.protocol.helper.DeviceKeyMemory
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestData
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.*
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.request.Protocol101Wrapper
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.GetPropGetStatusResponse
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.Protocol102Dps
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.codec.Hex
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.Scanner

@SpringBootTest(classes = [CaptureReader.Companion.ProvideJackson::class, RequestMemory::class, DeviceKeyMemory::class, MessageDecoder::class])
class CaptureReader {
    @Autowired
    lateinit var requestMemory: RequestMemory

    @Autowired
    lateinit var messageDecoder: MessageDecoder

    @Test
    @Disabled
    fun readCapture() {
        FileOutputStream("simple.csv").use { fos ->
            FileInputStream("capture.json").use {
                JsonFactory().createParser(it).use { jsonParser ->
                    var topic = ""
                    var msg = ""
                    while (null != jsonParser.nextToken()) {
                        val fieldName = jsonParser.currentName
                        if (fieldName == "mqtt.topic") {
                            jsonParser.nextToken()
                            topic = jsonParser.text
                        } else if (fieldName == "mqtt.msg") {
                            jsonParser.nextToken()
                            msg = jsonParser.text.split(":").joinToString("")
                            fos.write("$topic:$msg\n".toByteArray())
                        }
                    }
                }
            }
        }
    }

    @Test
//    @Disabled
    fun readSimple() {
        // Note: This test expects the deviceKeyMemory to contain the correct key for your device
        //       The key can be configured by setting override.device-memory[deviceId]=deviceKey
        //       in application.properties or application.yaml

        FileInputStream("stopTheCount.csv").use { fis ->
            Scanner(fis).use { scanner ->
                while (scanner.hasNextLine()) {
                    try {
                        val line = scanner.nextLine()
                        val (topic, message) = line.split(":")
                        val deviceId = topic.substring(topic.lastIndexOf("/") + 1)
                        when (val decodedMessage =
                            messageDecoder.decode(deviceId, ByteBuffer.wrap(Hex.decode(message)))) {
                            is Protocol101Wrapper -> {
                                println(
                                    "->  ${decodedMessage.dps.data.requestId} ${decodedMessage.dps.data.method} ${
                                        objectMapper.writeValueAsString(
                                            decodedMessage.dps.data.parameters
                                        )
                                    }"
                                )

                                if (decodedMessage.dps.data.security != null) {
                                    requestMemory[decodedMessage.dps.data.requestId] = RequestData(
                                        RequestMethod.valueOf(decodedMessage.dps.data.method.uppercase()),
                                        Hex.decode(decodedMessage.dps.data.security!!.nonce)
                                    )
                                } else {
                                    requestMemory[decodedMessage.dps.data.requestId] =
                                        RequestData(RequestMethod.valueOf(decodedMessage.dps.data.method.uppercase()))
                                }
                            }

                            is Protocol102Dps<*> -> {
                                if (decodedMessage.result is Array<*> && (decodedMessage.result as Array<*>)[0] is GetPropGetStatusResponse) {
                                    println(" <- ${decodedMessage.id} ${RequestMethod.GET_PROP.value} ${objectMapper.writeValueAsString(decodedMessage.result)}")
                                } else if (decodedMessage.result is ArrayNode || decodedMessage.result is JsonNode) {
                                    println(
                                        " <- ${decodedMessage.id} ${decodedMessage.method?.value} !!arbitrary!! ${
                                            objectMapper.writeValueAsString(
                                                decodedMessage.result
                                            )
                                        }"
                                    )
                                } else {
                                    println(" <- ${decodedMessage.id} ${objectMapper.writeValueAsString(decodedMessage.result)}")
                                }
                            }

                            is Response301 -> {
                                // not much to do with the map data in a test
                            }

                            else -> {
                                println("Unknown message type")
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
        class ProvideJackson {
            @Bean
            fun objectMapper(): ObjectMapper {
                return objectMapper
            }
        }

    }

}