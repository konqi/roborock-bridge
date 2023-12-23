package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.protocol.mqtt.response.map.Coordinate
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class MapDataForPublishTest {
    val testObj = MapDataForPublish(
        map = "Hi",
        robotPosition = Coordinate(0.0f, 0.0f),
        chargerPosition = Coordinate(0.0f, 0.0f),
        path = listOf(),
        predictedPath = listOf(),
        gotoPath = listOf()
    )

    @Test
    fun get() {
        testObj.getFields().forEach {
            assertThat(testObj[it]).isNotNull
        }
    }

    @Test
    fun getFields() {
        assertThat(testObj.getFields()).hasSameElementsAs(setOf(
            "map",
            "robotPosition",
            "chargerPosition",
            "path",
            "predictedPath",
            "gotoPath"
        ))
    }
}