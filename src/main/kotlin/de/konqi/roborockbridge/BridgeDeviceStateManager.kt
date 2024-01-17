package de.konqi.roborockbridge

import de.konqi.roborockbridge.bridge.interpreter.BridgeDeviceState
import de.konqi.roborockbridge.bridge.interpreter.InterpreterProvider
import de.konqi.roborockbridge.bridge.interpreter.getState
import de.konqi.roborockbridge.persistence.entity.Device
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BridgeDeviceStateManager(
    @Autowired private val interpreterProvider: InterpreterProvider
) {
    private val deviceStates: MutableMap<String, BridgeDeviceState> = mutableMapOf()

    fun updateDeviceState(device: Device) {
        deviceStates[device.deviceId] =
            interpreterProvider.getInterpreterForDevice(device)?.getState(device.state) ?: BridgeDeviceState.UNKNOWN
    }

    fun updateDeviceState(devices: List<Device>) {
        devices.forEach(this::updateDeviceState)
    }

    fun updateDeviceState(deviceId: String, states: Map<String, Int>) {
        deviceStates[deviceId] =
            interpreterProvider.getInterpreterForDevice(deviceId)?.getState(states) ?: BridgeDeviceState.UNKNOWN
    }

    fun setDeviceState(deviceId: String, state: BridgeDeviceState) {
        deviceStates[deviceId] = state
    }

    fun setAll(state: BridgeDeviceState) {
        deviceStates.keys.forEach { deviceStates[it] = state }
    }

    fun getDevicesInState(vararg state: BridgeDeviceState) =
        deviceStates.filter { device -> state.any { it == device.value } }.keys

    fun getDeviceIds() = deviceStates.keys
}