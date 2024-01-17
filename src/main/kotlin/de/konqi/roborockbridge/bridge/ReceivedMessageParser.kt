package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.bridge.interpreter.InterpreterProvider
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.*
import de.konqi.roborockbridge.utility.LoggerDelegate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
@Profile("bridge")
class ReceivedMessageParser(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val interpreterProvider: InterpreterProvider
) {
    fun parse(topic: String, payload: ByteArray): ReceivedMessage? {
        val topicWithoutBase = if (topic.startsWith(bridgeMqttConfig.baseTopic)) {
            topic.substring(bridgeMqttConfig.baseTopic.length)
        } else topic

        val header = ReceivedMessageHeader.fromTopic(topicWithoutBase)

        val payloadString = String(payload).trim()
        // must ignore options command with non-empty payload to avoid endless loop
        if(payloadString.isNotBlank() && header.command == CommandType.OPTIONS) {
            return null
        }
        val action = payloadString.let {
            try {
                if (it.startsWith("{") && it.endsWith("}")) {
                    objectMapper.readTree(it).get("action").textValue()
                } else {
                    it.split(",").first().trim()
                }
            } catch (e: Exception) {
                it.split(",").first().trim()
            }
        }.let { ActionKeywordsEnum.fromValue(it) }

        if (!isAcceptableCommand(targetType = header.targetType, command = header.command, action = action)) {
            logger.warn("Invalid command [targetType: ${header.targetType}, command: ${header.command}, action: $action]")
            return null
        }

        val expectedPayloadType = targetsAvailableForCommand[header.targetType]?.get(header.command)?.get(action)
        val dto: IpcRequestDTO = if (expectedPayloadType != null) {
            parseDto(payloadString, expectedPayloadType, header.deviceId) ?: StringDTO(String(payload))
        } else {
            StringDTO(String(payload))
        }

        return ReceivedMessage(
            header = header,
            body = ReceivedMessageBody(
                parameters = dto, actionKeyword = action
            ),
        )
    }

    private inline fun <reified T> parseDto(
        payloadString: String,
        expectedPayloadType: KClass<out T>,
        deviceId: String? = null
    ): T? where T : IpcRequestDTO {
        try {
            return payloadString
                .let(objectMapper::readTree)
                .let { jsonNode ->
                    // if device specific, preprocess json to convert string values to numeric values
                    deviceId?.let {
                        interpreterProvider.getInterpreterForDevice(deviceId)?.preprocessMapNode(jsonNode)
                    } ?: jsonNode
                }.let {
                    objectMapper.treeToValue(it, expectedPayloadType.java)
                }
        } catch (e: Exception) {
            logger.debug("Using default properties because action type should have json payload but json parser failed. $e")
            return expectedPayloadType.constructors.find { it.parameters.none { param -> !param.isOptional } }
                ?.callBy(mapOf())
        }
    }

    companion object {
        private val logger by LoggerDelegate()

        val targetsAvailableForCommand = mapOf(
            TargetType.DEVICE to mapOf(
                CommandType.ACTION to mapOf(
                    ActionKeywordsEnum.SEGMENTS to AppSegmentCleanRequestDTO::class,
                    ActionKeywordsEnum.CLEAN_MODE to SetCleanMotorModeDTO::class,
                    ActionKeywordsEnum.START to AppStartDTO::class,
                    ActionKeywordsEnum.PAUSE to null,
                    ActionKeywordsEnum.HOME to null
                ),
                CommandType.GET to mapOf(
                    ActionKeywordsEnum.STATE to null,
                    ActionKeywordsEnum.MAP to null
                ),
            ), TargetType.DEVICE_PROPERTY to mapOf(
                CommandType.SET to mapOf(),
                CommandType.OPTIONS to mapOf()
            ), TargetType.HOME to mapOf(
                CommandType.GET to mapOf()
            ), TargetType.ROUTINE to mapOf(
                CommandType.ACTION to mapOf()
            )
        )

        fun isAcceptableCommand(targetType: TargetType, command: CommandType, action: ActionKeywordsEnum) =
            if (targetsAvailableForCommand.containsKey(targetType) && targetsAvailableForCommand[targetType]!!.containsKey(
                    command
                )
            ) {
                val possibleActions = targetsAvailableForCommand[targetType]!![command]!!.keys
                if (possibleActions.isNotEmpty()) {
                    if (possibleActions.contains(action)) true
                    else true
                } else true
            } else false

    }

}