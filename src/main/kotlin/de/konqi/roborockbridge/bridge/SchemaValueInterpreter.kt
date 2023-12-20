package de.konqi.roborockbridge.bridge

interface SchemaValueInterpreter {
    fun interpret(schemaId: Int, value: Int): String
    fun getOptions(schemaId: Int): Map<Int, String>
}

class S8UltraInterpreter : SchemaValueInterpreter {
    override fun interpret(schemaId: Int, value: Int): String {
        return when (schemaId) {
            120 -> ERROR_CODE_120[value]
            121 -> DEVICE_STATES_101[value]
            // Battery
            122 -> "$value %"
            123 -> FAN_POWER_123[value]
            124 -> WATER_BOX_124[value]
            125 -> "$value %"
            126 -> "$value %"
            127 -> "$value %"
            // 128 -> 🤷
            133 -> if (value == 1) "charging" else "not charging"
            134 -> if (value == 1) "drying" else "not drying"

            else -> null

        } ?: "$value 🤷"
    }

    override fun getOptions(schemaId: Int): Map<Int, String> {
        return when(schemaId) {
            120 -> ERROR_CODE_120
            121 -> DEVICE_STATES_101
            123 -> FAN_POWER_123
            124 -> WATER_BOX_124
            else -> emptyMap()
        }
    }

    companion object {
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