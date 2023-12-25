package de.konqi.roborockbridge.bridge.interpreter

import org.springframework.stereotype.Service

@Service
class S8UltraInterpreter : SchemaValueInterpreter {
    override val name: String
        get() = "S8 Ultra Interpreter"

    override val modelNames: Set<String>
        get() = setOf("roborock.vacuum.a70")

    override fun interpret(code: String, value: Int): String {
        return when (code) {
            SCHEMA_TO_CODE_MAPPING[120] -> ERROR_CODE_120[value]
            SCHEMA_TO_CODE_MAPPING[121] -> DEVICE_STATES_101[value]
            // Battery
            SCHEMA_TO_CODE_MAPPING[122] -> "$value %"
            SCHEMA_TO_CODE_MAPPING[123] -> FAN_POWER_123[value]
            SCHEMA_TO_CODE_MAPPING[124] -> WATER_BOX_124[value]
            SCHEMA_TO_CODE_MAPPING[125] -> "$value %"
            SCHEMA_TO_CODE_MAPPING[126] -> "$value %"
            SCHEMA_TO_CODE_MAPPING[127] -> "$value %"
            // 128 -> 🤷
            SCHEMA_TO_CODE_MAPPING[133] -> if (value == 1) "charging" else "not charging"
            SCHEMA_TO_CODE_MAPPING[134] -> if (value == 1) "drying" else "not drying"

            else -> null

        } ?: "$value 🤷"
    }

    override fun getOptions(code: String): Map<Int, String> {
        return when (code) {
            SCHEMA_TO_CODE_MAPPING[120] -> ERROR_CODE_120
            SCHEMA_TO_CODE_MAPPING[121] -> DEVICE_STATES_101
            SCHEMA_TO_CODE_MAPPING[123] -> FAN_POWER_123
            SCHEMA_TO_CODE_MAPPING[124] -> WATER_BOX_124
            else -> emptyMap()
        }
    }

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
            0 to "Unknown",
            1 to "Initiating",
            2 to "Sleeping",
            3 to "Idle",
            4 to "Remote Control",
            5 to "Cleaning",
            6 to "Returning Dock",
            7 to "Manual Mode",
            8 to "Charging",
            9 to "Charging Error",
            10 to "Paused",
            11 to "Spot Cleaning",
            12 to "In Error",
            13 to "Shutting Down",
            14 to "Updating",
            15 to "Docking",
            16 to "Go To",
            17 to "Zone Clean",
            18 to "Room Clean",
            22 to "Emptying dust container",
            23 to "Washing mop",
            26 to "Going to wash mop",
            28 to "In call",
            29 to "Mapping",
            100 to "Fully Charged",
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
    }
}