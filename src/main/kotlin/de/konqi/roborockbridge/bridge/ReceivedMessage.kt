package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppSegmentCleanRequestDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.IpcRequestDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.SetCleanMotorModeDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.StringDTO
import de.konqi.roborockbridge.utility.LoggerDelegate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

enum class CommandType(val value: String) {
    /**
     * Poll for an update
     */
    GET("get"),

    /**
     * Set a value
     */
    SET("set"),

    /**
     * invoke an action or task
     */
    ACTION("action"),

    /**
     * unknown command (to be ignored)
     */
    UNKNOWN("unknown");

    companion object {
        private val mapping = CommandType.entries.associateBy(CommandType::value)
        fun fromValue(value: String?) = CommandType.mapping[value] ?: UNKNOWN
    }
}

enum class ActionKeywordsEnum(val value: String) {
    HOME("home"), MAP("map"), SEGMENTS("segments"), CLEAN_MODE("clean_mode"), STATE("state"), UNKNOWN("unknown");

    companion object {
        private val mapping = ActionKeywordsEnum.entries.associateBy(ActionKeywordsEnum::value)
        fun fromValue(value: String?) = ActionKeywordsEnum.mapping[value] ?: UNKNOWN
    }
}

enum class TargetType {
    UNKNOWN, HOME, DEVICE, DEVICE_PROPERTY, ROUTINE
}

data class ReceivedMessageHeader(
    val homeId: Int?,
    val deviceId: String?,
    val routineId: Int?,
    val property: String?,
    val command: CommandType = CommandType.UNKNOWN,
) {
    val targetType: TargetType
        get() = if (homeId != null) {
            if (property != null) TargetType.DEVICE_PROPERTY else if (deviceId != null) TargetType.DEVICE else if (routineId != null) TargetType.ROUTINE else TargetType.HOME
        } else TargetType.UNKNOWN

    val targetIdentifier: String? = if (property != null && deviceId != null && homeId != null) "$deviceId/$property"
    else if (deviceId != null && homeId != null) deviceId
    else if (routineId != null && homeId != null) routineId.toString()
    else homeId?.toString()

    companion object {
        private fun sectionRegex(sectionName: String, valuePattern: String = "[^/]+"): String =
            "(?:(?:$sectionName)/(?<$sectionName>$valuePattern))"

        private val commandSuffixRegex = "/(?<command>${
            listOf(
                CommandType.ACTION, CommandType.SET, CommandType.GET
            ).joinToString("|") { it.value }
        })?"

        private fun combineSections(vararg sections: String) = """(?:/(?:${sections.joinToString("|")}))*"""

        val deviceIdExtractionRegex = Regex(
            "${
                combineSections(
                    sectionRegex(BridgeMqtt.HOME, "[0-9]+"),
                    sectionRegex(BridgeMqtt.DEVICE),
                    sectionRegex(BridgeMqtt.ROUTINE)
                )
            }(?:/(?<${BridgeMqtt.DEVICE_PROPERTY}>[^/]+))?$commandSuffixRegex"
        )

        fun fromTopic(topic: String): ReceivedMessageHeader {
            val matches = deviceIdExtractionRegex.find(topic)

            return ReceivedMessageHeader(
                homeId = matches?.groups?.get(BridgeMqtt.HOME)?.value?.toInt(),
                deviceId = matches?.groups?.get(BridgeMqtt.DEVICE)?.value,
                routineId = matches?.groups?.get(BridgeMqtt.ROUTINE)?.value?.toInt(),
                property = matches?.groups?.get(BridgeMqtt.DEVICE_PROPERTY)?.value,
                command = CommandType.fromValue(matches?.groups?.get("command")?.value)
            )
        }
    }
}

data class ReceivedMessageBody(
    val parameters: IpcRequestDTO, val actionKeyword: ActionKeywordsEnum = ActionKeywordsEnum.UNKNOWN
)

data class ReceivedMessage(
    val header: ReceivedMessageHeader, val body: ReceivedMessageBody
)

@Service
@Profile("bridge")
class ReceivedMessageParser(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig, @Autowired private val objectMapper: ObjectMapper
) {
    fun parse(topic: String, payload: ByteArray): ReceivedMessage? {
        val topicWithoutBase = if (topic.startsWith(bridgeMqttConfig.baseTopic)) {
            topic.substring(bridgeMqttConfig.baseTopic.length)
        } else topic

        val header = ReceivedMessageHeader.fromTopic(topicWithoutBase)

        val action = String(payload).trim().let {
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
        val dto = if (expectedPayloadType != null) {
            objectMapper.readValue(String(payload), expectedPayloadType.java)
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

    companion object {
        private val logger by LoggerDelegate()

        val targetsAvailableForCommand = mapOf(
            TargetType.DEVICE to mapOf(
                CommandType.ACTION to mapOf(
                    ActionKeywordsEnum.SEGMENTS to AppSegmentCleanRequestDTO::class,
                    ActionKeywordsEnum.CLEAN_MODE to SetCleanMotorModeDTO::class,
                    ActionKeywordsEnum.HOME to null
                ),
                CommandType.GET to mapOf(
                    ActionKeywordsEnum.STATE to null, ActionKeywordsEnum.MAP to null
                ),
            ), TargetType.DEVICE_PROPERTY to mapOf(
                CommandType.SET to mapOf()
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