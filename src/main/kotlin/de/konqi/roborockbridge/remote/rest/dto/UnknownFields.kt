package de.konqi.roborockbridge.remote.rest.dto

import com.fasterxml.jackson.annotation.JsonAnySetter
import de.konqi.roborockbridge.utility.LoggerDelegate
import de.konqi.roborockbridge.utility.ObjectMapperDelegate

interface UnknownFields {
    @JsonAnySetter
    fun setOther(key: String, value: Any)
}

class UnknownFieldsImpl(private val prefix: String? = null) : UnknownFields {
    private val unknownFields = HashMap<String, Any>()

    override fun setOther(key: String, value: Any) {
        logger.warn(
            "${
                if (prefix.isNullOrBlank()) {
                    ""
                } else {
                    "$prefix: "
                }
            }Found unknown extra property \"$key\" with value: ${objectMapper.writeValueAsString(value)}"
        )
        unknownFields[key] = value
    }

    companion object {
        private val logger by LoggerDelegate()
        private val objectMapper by ObjectMapperDelegate()
    }
}