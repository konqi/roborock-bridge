package de.konqi.roborockbridge.protocol.mqtt

import de.konqi.roborockbridge.protocol.mqtt.raw.Message
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer

class MessageTest {
    @ParameterizedTest()
    @MethodSource("provideInputs")
    fun encodeAndDecode(random: UInt, timestamp: UInt, protocol: UShort, payload: ByteArray) {
        val message = Message().also {
            it.header.random = random
            it.header.timestamp = timestamp
            it.header.protocol = protocol
            it.payload = payload
        }
        val serialized = ByteBuffer.wrap(message.bytes)

        val deserialized = Message(serialized)

//        println("Test Payload:         ${String(payload)}")
//        println("Copied Payload:       ${String(message.payload)}")
//        println("Serialized Message:   ${String(serialized)}")
//        println("Deserialized Payload: ${String(deserialized.payload)}")
        assertTrue(message.payload contentEquals  payload, "Payload is pristine")
        assertTrue(deserialized.payload contentEquals payload, "Payload should not change")
        assertTrue(deserialized.header.sequenceNumber > 0u, "there should be a sequence number set")
        assertEquals(deserialized.header.random, random, "the random field must be set")
        assertEquals(deserialized.header.protocol, protocol, "the protocol field must be set")
        assertEquals(deserialized.header.timestamp, timestamp, "the timestamp field must be set")
        assertEquals(message.calculatedChecksum, deserialized.footer.checksum, "checksum at receiving end must match sender")
    }

    companion object {
        @JvmStatic
        fun provideInputs() = listOf(
            Arguments.of(1, 1, 1.toShort(), "This is a mighty string".toByteArray()),
            Arguments.of(
                UInt.MAX_VALUE.toInt(),
                UInt.MAX_VALUE.toInt(),
                UShort.MAX_VALUE.toShort(),
                "This is a mighty string".toByteArray()
            )
        )
    }
}