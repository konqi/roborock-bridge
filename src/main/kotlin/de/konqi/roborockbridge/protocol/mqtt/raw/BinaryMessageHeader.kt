package de.konqi.roborockbridge.protocol.mqtt.raw

import java.nio.ByteBuffer

class BinaryMessageHeader(private val buffer: ByteBuffer = ByteBuffer.allocate((HEADER_SIZE))) : BinaryProtocol {
    var protocolVersion: String
        get() = ByteArray(3).run {
            buffer.position(0)
            buffer.get(this, 0, size)
            String(this)
        }
        set(value) {
            buffer.apply {
                position(0)
                val protocolVersionBytes = value.toByteArray()
                if (protocolVersionBytes.size != 3) {
                    throw RuntimeException("protocol version must be exactly three bytes/chars")
                }
                put(protocolVersionBytes, 0, 3)
            }
        }

    var sequenceNumber: UInt
        get() = buffer.getInt(3).toUInt()
        set(value) {
            buffer.apply {
                position(3)
                putInt(value.toInt())
            }
        }

    var random: UInt
        get() = buffer.getInt(7).toUInt()
        set(value) {
            buffer.apply {
                position(7)
                putInt(value.toInt())
            }
        }

    var timestamp: UInt
        get() = buffer.getInt(11).toUInt()
        set(value) {
            buffer.apply {
                position(11)
                putInt(value.toInt())
            }
        }

    var protocol: UShort
        get() = buffer.getShort(15).toUShort()
        set(value) {
            buffer.apply {
                position(15)
                putShort(value.toShort())
            }
        }

    var payloadLength: UShort
        get() = buffer.getShort(17).toUShort()
        set(value) {
            buffer.apply {
                position(17)
                putShort(value.toShort())
            }
        }

    override val bytes: ByteArray
        get() {
            // ignore extraneous bytes in underlying array
            return buffer.array().copyOfRange(buffer.arrayOffset(), buffer.arrayOffset() + HEADER_SIZE)
        }

    companion object {
        const val HEADER_SIZE = 3 + 3 * UInt.SIZE_BYTES + 2 * UShort.SIZE_BYTES
    }
}