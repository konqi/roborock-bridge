package de.konqi.roborockbridge.protocol.mqtt

import de.konqi.roborockbridge.protocol.mqtt.raw.EncryptedMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.security.crypto.codec.Hex
import java.nio.ByteBuffer

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
        val decryptedMessage = EncryptedMessage(KEY, ByteBuffer.wrap(encryptedMessage.bytes))

        assertTrue(encryptedMessage.payload contentEquals payload)
        assertTrue(decryptedMessage.payload contentEquals payload)
    }

    @Test
    fun encodeTest() {
        val message = EncryptedMessage("secret")
        message.header.sequenceNumber = 1u
        message.header.protocol = 101u
        message.header.random = 4711u
        message.header.timestamp = 1234567890u
        message.payload = "Message".toByteArray()

        val expected = byteArrayOf(
            0x31,
            0x2e,
            0x30,
            0x00,
            0x00,
            0x00,
            0x01,
            0x00,
            0x00,
            0x12,
            0x67,
            0x49,
            0x96.toByte(),
            0x02,
            0xd2.toByte(),
            0x00,
            0x65,
            0x00,
            0x10,
            0x67,
            0xe2.toByte(),
            0xe2.toByte(),
            0xbd.toByte(),
            0xf5.toByte(),
            0xc1.toByte(),
            0x94.toByte(),
            0x4b,
            0x41,
            0x76,
            0x1d,
            0x71,
            0x37,
            0xa0.toByte(),
            0xe7.toByte(),
            0xab.toByte(),
            0xf5.toByte(),
            0x9b.toByte(),
            0x0a,
            0x47
        )

        assertEquals(String(Hex.encode(expected)), String(Hex.encode(message.bytes)))
    }

    @Test
    fun testEncodeTimestamp() {
        assertEquals("2d629940", EncryptedMessage.encodeTimestamp(1234567890))
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