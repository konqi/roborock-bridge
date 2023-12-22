package de.konqi.roborockbridge.bridge.interpreter

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

    companion object {
        val SCHEMA_TO_CODE_MAPPING = mapOf(
            120 to "error_code",
            121 to "state",
            122 to "battery",
            123 to "fan_power",
            124 to "water_box_mode",
            125 to "main_brush_life",
            126 to "side_brush_life",
            127 to "filter_life",
            128 to "additional_props",
            130 to "task_complete",
            131 to "task_cancel_low_power",
            132 to "task_cancel_in_motion",
            133 to "charging_state",
            134 to "drying_state"
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