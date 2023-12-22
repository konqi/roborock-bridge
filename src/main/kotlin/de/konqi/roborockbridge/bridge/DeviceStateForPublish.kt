package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.bridge.interpreter.SchemaValueInterpreter
import de.konqi.roborockbridge.persistence.entity.DeviceState

data class DeviceStateForPublish(
    val name: String,
    val value: String,
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