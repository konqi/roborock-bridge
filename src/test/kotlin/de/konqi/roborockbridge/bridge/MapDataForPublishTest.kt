package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.bridge.dto.MapDataForPublish
import de.konqi.roborockbridge.remote.mqtt.map.dto.Coordinate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MapDataForPublishTest {
    val testObj = MapDataForPublish(
        map = "Hi",
        bitmapData = "bitmap",
        robotPosition = Coordinate(0.0f, 0.0f),
        chargerPosition = Coordinate(0.0f, 0.0f),
        path = listOf(),
        predictedPath = listOf(),
        gotoPath = listOf(),
        virtualWalls = listOf(
            listOf(Coordinate(1.2f, 2.3f), Coordinate(3.4f, 4.5f)),
            listOf(Coordinate(6.7f, 7.8f), Coordinate(8.9f, 9.0f))
        )
    )

    @Test
    fun `can get all properties by their names`() {
        testObj.getFields().forEach {
            assertThat(testObj[it]).isNotNull
        }
    }

    @Test
    fun `can be serialized without issues`() {
        val objectMapper = jacksonObjectMapper()
        val jsonString = objectMapper.writeValueAsString(testObj)
        assertThat(jsonString).contains("virtualWalls")
        assertThat(jsonString).contains("bitmapData")
        println(jsonString)
    }

    @Test
    fun `correctly returns list of all properties defined in class`() {
        assertThat(testObj.getFields()).hasSameElementsAs(
            setOf(
                "map",
                "bitmapData",
                "robotPosition",
                "chargerPosition",
                "path",
                "predictedPath",
                "gotoPath",
                "virtualWalls"
            )
        )
    }
}