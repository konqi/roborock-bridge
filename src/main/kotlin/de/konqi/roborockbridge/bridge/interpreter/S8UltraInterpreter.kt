package de.konqi.roborockbridge.bridge.interpreter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.JsonNodeType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class S8UltraInterpreter : SchemaValueInterpreter {
    override val name: String
        get() = "S8 Ultra Interpreter"

    override val modelNames: Set<String>
        get() = setOf("roborock.vacuum.a70")

    override fun interpret(code: String, value: Int): String? {
        if (ENUM_LIKE_PROPERTIES_MAP.containsKey(code)) {
            return ENUM_LIKE_PROPERTIES_MAP[code]!![value]
        } else {
            return when (code) {
                // Battery
                BATTERY -> "$value %"
                MAIN_BRUSH_LIFE -> "$value %"
                SIDE_BRUSH_LIFE -> "$value %"
                FILTER_LIFE -> "$value %"
                // 128 -> 🤷
                CHARGING_STATE -> if (value == 1) "charging" else "not charging"
                DRYING_STATE -> if (value == 1) "drying" else "not drying"
                CLEAN_TIME -> "$value s"
                CLEAN_AREA -> "${BigDecimal(value).divide(BigDecimal(1_000_000), 2, RoundingMode.HALF_UP)} m^2"
                else -> null
            }
        }
    }

    override fun getOptions(code: String): Map<Int, String> = ENUM_LIKE_PROPERTIES_MAP[code] ?: emptyMap()

    override fun schemaIdToPropName(schemaId: Int): String? = SCHEMA_TO_CODE_MAPPING[schemaId]

    override fun getState(currentState: Map<String, Int>): BridgeDeviceState {
        return if (IDLE_STATES.any { currentState[STATE] == it }) {
            BridgeDeviceState.IDLE
        } else if (ACTIVE_STATES.any { currentState[STATE] == it }) {
            BridgeDeviceState.ACTIVE
        } else {
            BridgeDeviceState.UNKNOWN
        }
    }

    override fun preprocessMapNode(node: JsonNode): JsonNode = JsonNodeFactory.instance.objectNode().also { newNode ->
        for (fieldName in node.fieldNames()) {
            val currentNode = node.get(fieldName)
            val nodeType = currentNode.nodeType

            if (nodeType == JsonNodeType.STRING && ENUM_LIKE_PROPERTIES_MAP.containsKey(fieldName)) {
                val textValue = currentNode.textValue().trim().lowercase()

                val intValue = ENUM_LIKE_PROPERTIES_MAP[fieldName]!!.entries.find {
                    it.value.lowercase() == textValue
                }?.key
                if (intValue != null) {
                    newNode.put(fieldName, intValue)
                } else {
                    newNode.put(fieldName, currentNode.textValue())
                }
            } else {
                newNode.putIfAbsent(
                    fieldName,
                    currentNode
                )
            }
        }
    }

    companion object {
        const val ERROR_CODE = "error_code"
        const val STATE = "state"
        const val BATTERY = "battery"
        const val FAN_POWER = "fan_power"
        const val WATER_BOX_MODE = "water_box_mode"
        const val MAIN_BRUSH_LIFE = "main_brush_life"
        const val SIDE_BRUSH_LIFE = "side_brush_life"
        const val FILTER_LIFE = "filter_life"
        const val ADDITIONAL_PROPS = "additional_props"
        const val TASK_COMPLETE = "task_complete"
        const val TASK_CANCEL_LOW_POWER = "task_cancel_low_power"
        const val TASK_CANCEL_IN_MOTION = "task_cancel_in_motion"
        const val CHARGING_STATE = "charging_state"
        const val DRYING_STATE = "drying_state"
        const val MOP_MODE = "mop_mode"
        const val CLEAN_AREA = "clean_area"
        const val CLEAN_TIME = "clean_time"
        const val DOCK_ERROR_STATUS = "dock_error_status"

        val SCHEMA_TO_CODE_MAPPING = mapOf(
            120 to ERROR_CODE,
            121 to STATE,
            122 to BATTERY,
            123 to FAN_POWER,
            124 to WATER_BOX_MODE,
            125 to MAIN_BRUSH_LIFE,
            126 to SIDE_BRUSH_LIFE,
            127 to FILTER_LIFE,
            128 to ADDITIONAL_PROPS,
            130 to TASK_COMPLETE,
            131 to TASK_CANCEL_LOW_POWER,
            132 to TASK_CANCEL_IN_MOTION,
            133 to CHARGING_STATE,
            134 to DRYING_STATE
        )

        // Value to meaning maps:
        val DEVICE_STATES_101 = mapOf(
            0 to "unknown",
            1 to "initiating",
            2 to "sleeping",
            3 to "idle",
            4 to "remote control",
            5 to "cleaning",
            6 to "returning to dock",
            7 to "manual mode",
            8 to "charging",
            9 to "charging error",
            10 to "paused",
            11 to "spot cleaning",
            12 to "in error",
            13 to "shutting down",
            14 to "updating",
            15 to "docking",
            16 to "go to",
            17 to "zone clean",
            18 to "room clean",
            22 to "emptying dust container",
            23 to "washing mop",
            26 to "going to wash mop",
            28 to "in call",
            29 to "mapping",
            100 to "fully charged",
        )
        val IDLE_STATES = listOf(2, 3)
        val ACTIVE_STATES = listOf(4, 5, 6, 11, 15, 22, 23, 29)

        val ERROR_CODE_120 = mapOf(
            0 to "No error",
            1 to "Laser sensor fault",
            2 to "Collision sensor fault",
            3 to "Wheel floating",
            4 to "Cliff sensor fault",
            5 to "Main brush blocked",
            6 to "Side brush blocked",
            7 to "Wheel blocked",
            8 to "Device stuck",
            9 to "Dust bin missing",
            10 to "Filter blocked",
            11 to "Magnetic field detected",
            12 to "Low battery",
            13 to "Charging problem",
            14 to "Battery failure",
            15 to "Wall sensor fault",
            16 to "Uneven surface",
            17 to "Side brush failure",
            18 to "Suction fan failure",
            19 to "Unpowered charging station",
            20 to "Unknown Error",
            21 to "Laser pressure sensor problem",
            22 to "Charge sensor problem",
            23 to "Dock problem",
            24 to "No-go zone or invisible wall detected",
            39 to "Waste water tank missing",
            254 to "Bin full",
            255 to "Internal error",
        )
        val MOP_MODE_CODE_UNKNOWN = mapOf(
            300 to "Standard",
            301 to "Deep",
            303 to "Deep+",
        )
        val FAN_POWER_123 = mapOf(
            101 to "Quiet",
            102 to "Balanced",
            103 to "Turbo",
            104 to "Max",
            105 to "Off",
        )
        val WATER_BOX_124 = mapOf(
            200 to "Off",
            201 to "Mild",
            202 to "Moderate",
            203 to "Intense",
        )
//        val CAMERA_STATE = mapOf(
//            385 to "?"
//        )

        val ENUM_LIKE_PROPERTIES_MAP = mapOf(
            ERROR_CODE to ERROR_CODE_120,
            STATE to DEVICE_STATES_101,
            FAN_POWER to FAN_POWER_123,
            WATER_BOX_MODE to WATER_BOX_124,
            MOP_MODE to MOP_MODE_CODE_UNKNOWN,
            DOCK_ERROR_STATUS to ERROR_CODE_120
        )
    }
}