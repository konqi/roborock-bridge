package de.konqi.roborockbridge.bridge.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import de.konqi.roborockbridge.bridge.interpreter.SchemaValueInterpreter
import de.konqi.roborockbridge.persistence.entity.DeviceState

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeviceStateForPublish(
    @get:JsonIgnore
    val name: String,
    val value: String?,
    val rawValue: Int,
//    val possibleValues: Map<Int, String>
) {
    companion object {
        fun fromDeviceStateEntity(
            deviceState: DeviceState,
            interpreter: SchemaValueInterpreter
        ): DeviceStateForPublish = DeviceStateForPublish(
            name = deviceState.code,
            value = interpreter.interpret(code = deviceState.code, value = deviceState.value),
//            possibleValues = interpreter.getOptions(code = deviceState.code),
            rawValue = deviceState.value,
        )
    }
}