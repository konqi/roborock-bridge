package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MessageTest {
    @ParameterizedTest()
    @MethodSource("provideInputs")
    fun encodeAndDecode(sequenceNumber: UInt, random: UInt, timestamp: UInt, protocol: UShort, payload: ByteArray) {
        val serialized = Message().also {
            it.header.random = random
            it.header.timestamp = timestamp
            it.header.protocol = protocol
            it.payload = payload
        }.bytes

        val deserialized = Message(serialized)
        assertTrue(deserialized.payload contentEquals payload, "Payload should not change")
        assertTrue(deserialized.header.sequenceNumber > 0u, "there should be a sequence number set")
        assertEquals(deserialized.header.random, random, "the random field must be set")
        assertEquals(deserialized.header.protocol, protocol, "the protocol field must be set")
        assertEquals(deserialized.header.timestamp, timestamp, "the timestamp field must be set")
    }

    companion object {
        @JvmStatic
        fun provideInputs() = listOf(
            Arguments.of(1, 1, 1, 1.toShort(), "This is a mighty string".toByteArray()),
            Arguments.of(
                UInt.MAX_VALUE.toInt(),
                UInt.MAX_VALUE.toInt(),
                UInt.MAX_VALUE.toInt(),
                UShort.MAX_VALUE.toShort(),
                "This is a mighty string".toByteArray()
            )
        )
    }
}