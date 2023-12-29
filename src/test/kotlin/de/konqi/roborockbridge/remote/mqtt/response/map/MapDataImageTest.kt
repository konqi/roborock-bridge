package de.konqi.roborockbridge.remote.mqtt.response.map

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.ByteBuffer
import java.util.*

class MapDataImageTest {
    @Test
    fun `can decode image payload`() {
        val section = MapDataSection.fromRaw(payload)
        val mapDataImage = MapDataImage(section)

        assertDoesNotThrow {
            mapDataImage.getImageDataUrl()
        }
        assertThat(mapDataImage.rooms).hasSize(9)
        assertThat(mapDataImage.height).isGreaterThan(0u)
        assertThat(mapDataImage.width).isGreaterThan(0u)
    }

    companion object {
        val payload = ByteBuffer.wrap(
            Base64.getDecoder()
                .decode(MapDataImageTest::class.java.getResource("ImageDataSection.base64.txt")!!.readBytes())
        )
    }
}