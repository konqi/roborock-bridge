package de.konqi.roborockbridge.utility

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter

abstract class NestedJsonDeserializer<T>(private val clazz:Class<T>) : Converter<String?, T?> {
    override fun convert(value: String?): T {
        return try {
            objectMapper.readValue(value, clazz)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    override fun getInputType(typeFactory: TypeFactory): JavaType {
        return typeFactory.constructSimpleType(String::class.java, null)
    }

    override fun getOutputType(typeFactory: TypeFactory): JavaType {
        return typeFactory.constructSimpleType(clazz, null)
    }
}