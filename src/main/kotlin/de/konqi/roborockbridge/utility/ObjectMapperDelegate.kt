package de.konqi.roborockbridge.utility

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ObjectMapperDelegate: ReadOnlyProperty<Any,ObjectMapper> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = objectMapper

    companion object {
        val objectMapper = jacksonObjectMapper()
    }
}
