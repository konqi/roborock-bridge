package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class EncryptedMessageTest {

    @ParameterizedTest()
    @MethodSource("provideInputs")
    fun encodeAndDecode(sequenceNumber: UInt, random: UInt, timestamp: UInt, protocol: UShort, payload: ByteArray) {
        val encryptedMessage = EncryptedMessage(KEY).also {
            it.header.sequenceNumber = sequenceNumber
            it.header.random = random
            it.header.timestamp = timestamp
            it.header.protocol = protocol
            it.payload = payload
        }
        val decryptedMessage = EncryptedMessage(KEY, encryptedMessage.bytes)

        // payload must look different
        assertFalse(encryptedMessage.payload contentEquals payload)
        assertTrue(decryptedMessage.payload contentEquals payload)
    }

    companion object {
        const val KEY = "this is a secret key"

        @JvmStatic
        fun provideInputs() = listOf(
            Arguments.of(1, 1, 1, 1.toShort(), "All values 1".toByteArray()),
            Arguments.of(
                UInt.MAX_VALUE.toInt(),
                UInt.MAX_VALUE.toInt(),
                UInt.MAX_VALUE.toInt(),
                UShort.MAX_VALUE.toShort(),
                "All values Max".toByteArray()
            )
        )
    }
}