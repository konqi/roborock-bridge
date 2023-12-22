package de.konqi.roborockbridge.bridge.interpreter

/**
 * Interface for device state value interpreters
 *
 * <strong>Important:</strong>
 *
 * `code` is NOT a freely assignable string.
 * There are two places where you'll find those values:
 *
 * 1. The REST APIs contain `code` and a corresponding `schemaId`.
 * 2. Mqtt messages contain json properties with the same name as key.
 *
 * Only if the mapping is correct, updates via rest and mqtt are correctly interpreted by the bridge.
 */
interface SchemaValueInterpreter {
    /**
     * Name of the Interpreter
     */
    val name: String

    /**
     * A set of model names this interpreter is applicable for
     */
    val modelNames: Set<String>

    /**
     * Function that translates code and value into something for human consumption
     *
     */
    fun interpret(code: String, value: Int): String

    /**
     * Function to get possible values for a code
     */
    fun getOptions(code: String): Map<Int, String>

    /**
     * Function to translate schemaId to code
     */
    fun schemaIdToPropName(schemaId: Int): String?
}

