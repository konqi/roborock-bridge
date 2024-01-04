package de.konqi.roborockbridge.remote.mqtt

import com.fasterxml.jackson.annotation.JsonProperty

// set_clean_motor_mode [{"fan_power":101,"mop_mode":300,"water_box_mode":200}]
data class SetCleanMotorModePayload(
    @get:JsonProperty("fan_power")
    val fanPower: Int,
    @get:JsonProperty("mop_mode")
    val mopMode: Int,
    @get:JsonProperty("water_box_mode")
    val waterBoxMode: Int,
)
