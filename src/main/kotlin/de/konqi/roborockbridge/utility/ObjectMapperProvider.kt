package de.konqi.roborockbridge.utility

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// Not pretty, but better than creating an instance for each conversion
val objectMapper = jacksonObjectMapper()