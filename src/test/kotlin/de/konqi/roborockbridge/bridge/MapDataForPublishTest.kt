package de.konqi.roborockbridge.bridge

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.remote.mqtt.response.map.Coordinate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MapDataForPublishTest {
    val testObj = MapDataForPublish(
        map = "Hi",
        robotPosition = Coordinate(0.0f, 0.0f),
        chargerPosition = Coordinate(0.0f, 0.0f),
        path = listOf(),
        predictedPath = listOf(),
        gotoPath = listOf(),
        virtualWalls = listOf(Coordinate(1.2f, 2.3f) to Coordinate(3.4f, 4.5f),
            Coordinate(6.7f, 7.8f) to Coordinate(8.9f, 9.0f))
    )

    @Test
    fun `can get all properties by their names`() {
        testObj.getFields().forEach {
            assertThat(testObj[it]).isNotNull
        }
    }

    @Test
    fun `can be serialized without issues`(){
        val objectMapper = jacksonObjectMapper()
        val jsonString = objectMapper.writeValueAsString(testObj)
        assertThat(jsonString).contains("virtual_walls")
        println(jsonString)
    }

    @Test
    fun `correctly returns list of all properties defined in class`() {
        assertThat(testObj.getFields()).hasSameElementsAs(
            setOf(
                "map",
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