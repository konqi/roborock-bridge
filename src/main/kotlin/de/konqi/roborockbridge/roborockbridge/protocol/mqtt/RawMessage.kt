package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.nio.ByteBuffer
import java.util.zip.CRC32

class RawMessage(
    val sequenceNumber: UInt,
    val random: UInt,
    val timestamp: UInt,
    val protocol: UShort,
    val payload: ByteArray,
) {
    private val protocolVersion: String = "1.0"
    private val bufferSize: Int get() = 3 * UInt.SIZE_BYTES + 2 * UShort.SIZE_BYTES + protocolVersion.length + payload.size + Long.SIZE_BYTES
    private val byteBuffer: ByteBuffer = ByteBuffer.allocate(bufferSize)
    private val checksum: Long
        get() = CRC32().run {
            update(byteBuffer.array(), 0, bufferSize - 4)
            value
        }

    fun serialize(): ByteArray {
        // JVM multi-byte data types are big endian (no conversion required)
        return byteBuffer.apply {
            val protocolVersionBytes = protocolVersion.toByteArray()
            put(protocolVersion.toByteArray(), 0, protocolVersionBytes.size) // 3

            // @Todo need to increase sequence number
            putInt(sequenceNumber.and(UInt.MAX_VALUE).toInt()) // 7

            // @Todo need to update random value
            putInt(random.and(UInt.MAX_VALUE).toInt()) // 11

            putInt(timestamp.toInt()) // 15

            putShort(protocol.toShort()) // 17

            putShort(payload.size.toShort()) // 19

            put(payload)

            putLong(checksum)
        }.array()
    }

    companion object {
        class ByteArrayMessage(private val input: ByteArray) {
            private val byteBuffer: ByteBuffer = ByteBuffer.wrap(input)

            val protocolVersion: String
                get() = ByteArray(3).run {
                    byteBuffer.get(this, 0, size)
                    String(this)
                }
            val sequenceNumber: UInt get() = byteBuffer.getInt(3).toUInt()
            val random: UInt get() = byteBuffer.getInt(7).toUInt()
            val timestamp: UInt get() = byteBuffer.getInt(11).toUInt()
            val protocol: UShort get() = byteBuffer.getShort(15).toUShort()
            private val payloadLength: UShort get() = byteBuffer.getShort(17).toUShort()
            val payload: ByteArray get() = ByteArray(payloadLength.toInt()).also {
                byteBuffer.position(19)
                byteBuffer.get(it) }
            val checksum: Long get() = byteBuffer.getLong(input.size - Long.SIZE_BYTES)
            val calculatedChecksum: Long
                get() = CRC32().run {
                    update(byteBuffer.array(), 0, input.size - 4)
                    value
                }
        }

        fun deserialize(input: ByteArray): RawMessage {
            val byteMessage = ByteArrayMessage(input)

            if (byteMessage.checksum != byteMessage.calculatedChecksum) {
                throw RuntimeException("Checksum mismatch")
            }

            if(byteMessage.protocolVersion != "1.0") {
                throw RuntimeException("Protocol version mismatch")
            }

            return RawMessage(
                sequenceNumber = byteMessage.sequenceNumber,
                random = byteMessage.random,
                timestamp = byteMessage.timestamp,
                protocol = byteMessage.protocol,
                payload = byteMessage.payload,
            )
        }
    }

}

