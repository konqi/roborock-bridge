package de.konqi.roborockbridge.utility

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

abstract class NestedJsonSerializer<T>(private val clazz: Class<T>) : Converter<T?, String> {
    override fun convert(value: T?): String {
        return try {
            objectMapper.writeValueAsString(value)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    override fun getInputType(typeFactory: TypeFactory): JavaType {
        return typeFactory.constructSimpleType(clazz, null)
    }

    override fun getOutputType(typeFactory: TypeFactory): JavaType {
        return typeFactory.constructSimpleType(String::class.java, null)
    }
}