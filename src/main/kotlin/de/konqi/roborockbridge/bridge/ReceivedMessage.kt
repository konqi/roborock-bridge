package de.konqi.roborockbridge.bridge

import org.springframework.beans.factory.annotation.Autowired
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
    HOME("home"),
    UNKNOWN("unknown");

    companion object {
        private val mapping = ActionKeywordsEnum.entries.associateBy(ActionKeywordsEnum::value)
        fun fromValue(value: String?) = ActionKeywordsEnum.mapping[value] ?: UNKNOWN
    }
}

enum class TargetType {
    UNKNOWN,
    HOME,
    DEVICE,
    ROUTINE
}

data class ReceivedMessage(
    val homeId: Int?,
    val deviceId: String?,
    val routineId: Int?,
    val command: CommandType = CommandType.UNKNOWN,
    val parameters: String = "",
    val actionKeyword: ActionKeywordsEnum = ActionKeywordsEnum.UNKNOWN
) {
    val targetType: TargetType = if (homeId != null) {
        if (deviceId != null) TargetType.DEVICE else if (routineId != null) TargetType.ROUTINE else TargetType.HOME
    } else TargetType.UNKNOWN

    val targetIdentifier: String? = if (homeId != null) {
        deviceId ?: (routineId?.toString() ?: homeId.toString())
    } else null
}

@Service
class ReceivedMessageParser(
    @Autowired private val bridgeMqttConfig: BridgeMqttConfig
) {
    private final fun sectionRegex(sectionName: String, valuePattern: String = "[^/]+"): String {
        return "(?:(?:$sectionName)/(?<$sectionName>$valuePattern))"
    }

    val deviceIdExtractionRegex = Regex(
        "${Regex.escape(bridgeMqttConfig.baseTopic)}(?:/(?:${
            sectionRegex(
                BridgeMqtt.HOME,
                "[0-9]+"
            )
        }|${sectionRegex(BridgeMqtt.DEVICE)}))*(?:/(?<surplus>.*))?"
    )

    fun parse(topic: String, payload: ByteArray): ReceivedMessage {
        val matches = deviceIdExtractionRegex.find(topic)

        val surplus = matches?.groups?.get("surplus")?.value?.split("/")
        val parameters = String(payload).trim()
        return ReceivedMessage(
            homeId = matches?.groups?.get(BridgeMqtt.HOME)?.value?.toInt(),
            deviceId = matches?.groups?.get(BridgeMqtt.DEVICE)?.value,
            routineId = matches?.groups?.get(BridgeMqtt.ROUTINE)?.value?.toInt(),
            command = CommandType.fromValue(surplus?.last()),
            parameters = parameters,
            actionKeyword = ActionKeywordsEnum.fromValue(parameters)

        )
    }
}


