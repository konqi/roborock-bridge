package de.konqi.roborockbridge.bridge.interpreter

import org.springframework.stereotype.Service

@Service
class GenericInterpreter : SchemaValueInterpreter {
    override val name: String
        get() = "Generic Interpreter"

    override val modelNames: Set<String>
        get() = setOf("*")

    override fun interpret(code: String, value: Int): String? = null

    override fun getOptions(code: String): Map<Int, String> = emptyMap()

    override fun schemaIdToPropName(schemaId: Int): String? = "$schemaId"

    override fun getState(currentState: Map<String, Int>): BridgeDeviceState {
        return if (IDLE_STATES.any { currentState["state"] == it }) {
            BridgeDeviceState.IDLE
        } else if (ACTIVE_STATES.any { currentState["state"] == it }) {
            BridgeDeviceState.ACTIVE
        } else {
            BridgeDeviceState.UNKNOWN
        }
    }

    companion object {
        val IDLE_STATES = listOf(2, 3)
        val ACTIVE_STATES = listOf(4, 5, 6, 11, 15, 22, 23, 29)
    }
}