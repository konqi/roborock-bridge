package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class RawMessageTest {
    @ParameterizedTest()
    @MethodSource("provideInputs")
    fun encodeAndDecode(sequenceNumber: UInt, random: UInt, timestamp: UInt, protocol: UShort, payload: ByteArray) {
        val serialized = RawMessage(sequenceNumber, random, timestamp, protocol, payload).serialize()

        val deserialized = RawMessage.deserialize(serialized)
        assertTrue(deserialized.payload contentEquals payload)
        assertEquals(deserialized.sequenceNumber, sequenceNumber)
        assertEquals(deserialized.random, random)
        assertEquals(deserialized.protocol, protocol)
    }

    companion object {
        @JvmStatic
        fun provideInputs() = listOf(Arguments.of( 1u, 1u, 1u, 1u.toUShort(), "This is a mighty string".toByteArray()))
    }
}