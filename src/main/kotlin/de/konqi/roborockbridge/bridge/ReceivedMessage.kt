package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.IpcRequestDTO

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
     * query about possible values of device property
     */
    OPTIONS("options"),

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
    MAP("map"),
    SEGMENTS("segments"),
    CLEAN_MODE("clean_mode"),
    START("start"),
    PAUSE("pause"),
    STATE("state"),
    UNKNOWN("unknown");

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

    val targetIdentifier: String? = if (property != null && deviceId != null && homeId != null) property
    else if (deviceId != null && homeId != null) deviceId
    else if (routineId != null && homeId != null) routineId.toString()
    else homeId?.toString()

    companion object {
        private fun sectionRegex(sectionName: String, valuePattern: String = "[^/]+"): String =
            "(?:(?:$sectionName)/(?<$sectionName>$valuePattern))"

        private val commandSuffixRegex = "/(?<command>${
            CommandType.entries.filter { it != CommandType.UNKNOWN }.joinToString("|") { it.value }
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

