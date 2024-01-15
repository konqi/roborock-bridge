package de.konqi.roborockbridge.remote.mqtt.map.dto

import de.konqi.roborockbridge.utility.ObjectMapperDelegate
import de.konqi.roborockbridge.utility.base64
import de.konqi.roborockbridge.utility.deflate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.InflaterInputStream

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

    @Test
    fun `can get compressed bitmap`() {
        val section = MapDataSection.fromRaw(payload)
        val mapDataImage = MapDataImage(section)

        val bitmap = mapDataImage.getCompressedBitmapData()
        assertThat(bitmap.mode).isEmpty()

        // validate serializations
        objectMapper.writeValueAsString(bitmap)
            .also {
                assertThat(it).containsSequence("\"data\":")
                assertThat(it).containsSequence("\"meta\":")
                assertThat(it).containsSequence("\"mode\":")
            }

        val compressed = bitmap.deflate().base64()
        assertThat(compressed.mode).isEqualTo(listOf("deflate", "base64"))
        val decoded = ByteArrayOutputStream().use {
            InflaterInputStream(
                Base64
                    .getDecoder()
                    .decode(compressed.data).inputStream()
            ).transferTo(it)

            it.toByteArray()
        }

        // even base64 encoded the compressed data should be less than the original
        assertThat(compressed.data.length).isLessThan(bitmap.data.size)
//        println("Compression grade ${100 - (compressed.data.length * 100f / bitmap.data.size.toFloat())} %")
//        println("Difference ${bitmap.data.size - compressed.data.length}")
//        println("Before: ${bitmap.data.size}, After: ${compressed.data.length}")

        assertThat(decoded).isEqualTo(bitmap.data)
    }

    companion object {
        val objectMapper by ObjectMapperDelegate()
        val payload = ByteBuffer.wrap(
            Base64.getDecoder()
                .decode(MapDataImageTest::class.java.getResource("ImageDataSection.base64.txt")!!.readBytes())
        )
    }
}