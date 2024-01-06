package de.konqi.roborockbridge

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import de.konqi.roborockbridge.persistence.DeviceRepository
import de.konqi.roborockbridge.persistence.HomeRepository
import de.konqi.roborockbridge.persistence.entity.Device
import de.konqi.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.remote.helper.RequestData
import de.konqi.roborockbridge.remote.helper.RequestMemory
import de.konqi.roborockbridge.remote.mqtt.MessageDecoder
import de.konqi.roborockbridge.remote.mqtt.MessageWrapper
import de.konqi.roborockbridge.remote.mqtt.RequestMethod
import de.konqi.roborockbridge.remote.mqtt.StatusUpdate
import de.konqi.roborockbridge.remote.mqtt.ipc.request.IpcRequestWrapper
import de.konqi.roborockbridge.remote.mqtt.ipc.response.payload.GetPropGetStatusResponse
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseDps
import de.konqi.roborockbridge.remote.mqtt.ipc.response.IpcResponseWrapper
import de.konqi.roborockbridge.remote.mqtt.response.MapDataWrapper
import de.konqi.roborockbridge.remote.mqtt.response.Protocol301
import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.utility.cast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.system.exitProcess


@ConfigurationProperties(prefix = "capture-reader")
data class CaptureReaderConfiguration(
    val devices: Map<String, String>?,
//    val wiresharkExport: String = "captures/simple.json",
//    val csvFile: String = "captures/simple.csv"
)

/**
 * This class is used to decode captures of the mqtt communication by the roborock app
 * You have to add the device key of your robot to the configuration file. The structure
 * should be:
 * ```capture-reader.devices[deviceId]=deviceKey```
 *
 * TODO: Make app print device key on first run otherwise find device key via mqtt
 */
@Service
@EnableConfigurationProperties(CaptureReaderConfiguration::class)
@Profile("capture-reader")
class CaptureReader(
    @Autowired val requestMemory: RequestMemory,
    @Autowired val messageDecoder: MessageDecoder,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val captureReaderConfiguration: CaptureReaderConfiguration,
    @Autowired val appArgs: ApplicationArguments,
    val deviceRepository: DeviceRepository,
    val homeRepository: HomeRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    fun run() {
        prepareEnv()
        val inputFile = appArgs.getOptionValues("input")?.first()
        val outputFile = appArgs.getOptionValues("output")?.first()

        val mode = appArgs.getOptionValues("mode").first()
        if (mode == MODE_JSON_TO_CSV) {
            if (inputFile != null && outputFile != null) {
                readWiresharkCaptureJson(inputFile, outputFile)
                logger.info("Done.")
            } else {
                logger.error("No input and/or output file specified. Please specify with option: --input=<path_to_file> --output=<path_to_file>")
                logUsage()
            }
        } else if (mode == MODE_DECODE_CSV) {
            if (deviceRepository.count() < 1) {
                logger.error("Cannot decode without device keys. Either provide them via configuration or command line option: --device=<deviceId>:<deviceKey>")
                logUsage()
                exitProcess(-1)
            }
            if (inputFile != null) {
                decodeCsvFile(inputFile)
                logger.info("Done.")
            } else {
                logger.error("No input file specified. Please specify with option: --input=<path_to_file>")
                logUsage()
            }
        } else {
            logger.error("Invalid or no mode specified.")
            logUsage()
        }

        exitProcess(0)
    }

    fun logUsage() {
        logger.error("Usage:")
        logger.error("--mode=<$MODE_JSON_TO_CSV|$MODE_DECODE_CSV>                       (required) Mode of operation")
        logger.error("--input=<path_to_file>                       (required) Input file path e.g.: captures/file.json (do not use spaces or escape strings)")
        logger.error("--output=<path_to_file>                      (required for mode $MODE_JSON_TO_CSV) Output file path e.g.: captures/file.csv (do not use spaces or escape strings)")
        logger.error("--device=<deviceId>:<deviceKey>              (required if keys not provided via configuration) Must be device/key combination used for capturing network traffic")
    }

    fun prepareEnv() {
        val fakeHome = homeRepository.save(Home(homeId = 0, name = "Fake Home"))
        captureReaderConfiguration.devices?.map { (deviceId, deviceKey) ->
            Device(
                deviceId = deviceId,
                deviceKey = deviceKey,
                state = emptyList(),
                serialNumber = "",
                model = "",
                firmwareVersion = "",
                productName = "",
                name = "",
                home = fakeHome
            )
        }?.also {
            deviceRepository.saveAll(it)
        }

        appArgs.getOptionValues("device")?.map {
            val (deviceId, deviceKey) = it.split(":")
            Device(
                deviceId = deviceId,
                deviceKey = deviceKey,
                state = emptyList(),
                serialNumber = "",
                model = "",
                firmwareVersion = "",
                productName = "",
                name = "",
                home = fakeHome
            )
        }?.also {
            deviceRepository.saveAll(it)
        }
    }

    fun readWiresharkCaptureJson(inputCaptureJsonFile: String, outputCsvFile: String) {
        FileInputStream(inputCaptureJsonFile).use {
            FileOutputStream(outputCsvFile).use { fos ->
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

    fun decodeCsvFile(inputCsvFile: String) {
        FileInputStream(inputCsvFile).use { fis ->
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
                                        requestMemory.put(
                                            ipcRequest.payload.dps.data.requestId, RequestData(
                                                RequestMethod.valueOf(ipcRequest.payload.dps.data.method.uppercase()),
                                                Hex.decode(ipcRequest.payload.dps.data.security!!.nonce)
                                            )
                                        )
                                    } else {
                                        requestMemory.put(
                                            ipcRequest.payload.dps.data.requestId,
                                            RequestData(RequestMethod.valueOf(ipcRequest.payload.dps.data.method.uppercase()))
                                        )
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
                                        println(
                                            " <- ${ipcResponse.payload.id} ${
                                                objectMapper.writeValueAsString(
                                                    ipcResponse.payload.result
                                                )
                                            }"
                                        )
                                    }
                                }

                                MapDataWrapper.SCHEMA_TYPE -> {
                                    val mapResponse = cast<MessageWrapper<Protocol301>>(decodedMessage)
                                    if (mapResponse.payload.payload.map != null) {
                                        val base64Url = mapResponse.payload.payload.map.getImageDataUrl()
                                        println(" <- Image Url: $base64Url")
                                    }
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
        const val MODE_JSON_TO_CSV = "reduce"
        const val MODE_DECODE_CSV = "decode"
        val logger by LoggerDelegate()
//        val objectMapper = jacksonObjectMapper()
//
//        @EnableConfigurationProperties(CaptureReaderConfiguration::class)
//        class ProvideJackson(@Autowired val knownDevices: CaptureReaderConfiguration) {
//            @Bean
//            fun objectMapper(): ObjectMapper {
//                return objectMapper
//            }
//
//            @Bean
//            fun robotRepository(): DeviceRepository {
//                val mock = Mockito.mock(DeviceRepository::class.java)
//                Mockito.`when`(mock.findById(Mockito.anyString())).then {
//                    val deviceId = it.arguments.first().toString()
//                    Optional.of(
//                        Device(
//                            deviceId = deviceId,
//                            deviceKey = knownDevices.devices[deviceId]
//                                ?: throw RuntimeException("Must configure device $deviceId in properties"),
//                            state = emptyList(),
//                            serialNumber = "",
//                            model = "",
//                            firmwareVersion = "",
//                            productName = "",
//                            name = "",
//                            home = Home(homeId = 0, name = "")
//
//                        )
//                    )
//                }
//                return mock
//            }
//        }
//
    }
}