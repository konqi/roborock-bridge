package de.konqi.roborockbridge.roborockbridge.state

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Device
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Product
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Exception

data class Robot(
    val deviceId: String,
    val deviceName: String,
    val deviceInformation: Device,
    val productInformation: Product
)

@Component
class Robots(
    @Autowired
    private var objectMapper: ObjectMapper
) {
    final val robots: MutableList<Robot> = mutableListOf()

    @PostConstruct
    fun restore() {
        try {
            FileInputStream(ROBOTS_FILENAME).use { fis ->
                robots.addAll(objectMapper.readValue<List<Robot>>(fis))
            }
        } catch (e: FileNotFoundException) {
            logger.info("Could not load robots. No file '$ROBOTS_FILENAME' found.")
        }
    }

    @PreDestroy
    fun save() {
        try {
            FileOutputStream(ROBOTS_FILENAME).use { fos -> objectMapper.writeValue(fos, robots) }
        } catch (e: Exception) {
            logger.info("Could not save robots to file '$ROBOTS_FILENAME', ${e.message}.")
        }
    }

    companion object {
        private const val ROBOTS_FILENAME = "robots.json"
        private val logger by LoggerDelegate()
    }
}