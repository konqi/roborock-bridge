package de.konqi.roborockbridge.roborockbridge.state

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Room
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Exception

@Component
class Home(
    @Autowired
    private var objectMapper: ObjectMapper
) {
    final val rooms: MutableList<Room> = mutableListOf()

    @PostConstruct
    fun restore() {
        try {
            FileInputStream(HOME_FILENAME).use { fis ->
                rooms.addAll(objectMapper.readValue<List<Room>>(fis))
            }
        } catch (e: FileNotFoundException) {
            logger.info("Could not load home. No file '$HOME_FILENAME' found.")
        }
    }

    @PreDestroy
    fun save() {
        try {
            FileOutputStream(HOME_FILENAME).use { fos -> objectMapper.writeValue(fos, rooms) }
        } catch (e: Exception) {
            logger.info("Could not save home to file '$HOME_FILENAME', ${e.message}.")
        }
    }

    companion object {
        private const val HOME_FILENAME = "user-home.json"
        private val logger by LoggerDelegate()
    }
}
