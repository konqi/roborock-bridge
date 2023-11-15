package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.nio.ByteBuffer

class BinaryMessageHeader(private val headerBuffer: ByteBuffer = ByteBuffer.allocate((HEADER_SIZE))) : BinaryProtocol {
    var protocolVersion: String
        get() = ByteArray(3).run {
            headerBuffer.position(0)
            headerBuffer.get(this, 0, size)
            String(this)
        }
        set(value) {
            headerBuffer.run {
                position(0)
                val protocolVersionBytes = value.toByteArray()
                // TODO: assert protocolVersionByes.size == 3
                put(protocolVersionBytes, 0, 3)
            }
        }

    var sequenceNumber: UInt
        get() = headerBuffer.getInt(3).toUInt()
        set(value) {
            headerBuffer.run {
                position(3)
                putInt(value.toInt())
            }
        }

    var random: UInt
        get() = headerBuffer.getInt(7).toUInt()
        set(value) {
            headerBuffer.run {
                position(7)
                putInt(value.toInt())
            }
        }

    var timestamp: UInt
        get() = headerBuffer.getInt(11).toUInt()
        set(value) {
            headerBuffer.run {
                position(11)
                putInt(value.toInt())
            }
        }

    var protocol: UShort
        get() = headerBuffer.getShort(15).toUShort()
        set(value) {
            headerBuffer.run {
                position(15)
                putShort(value.toShort())
            }
        }

    var payloadLength: UShort
        get() = headerBuffer.getShort(17).toUShort()
        set(value) {
            headerBuffer.run {
                position(17)
                putShort(value.toShort())
            }
        }

    override val bytes: ByteArray
        get() = headerBuffer.array()

    companion object {
        const val HEADER_SIZE = 3 + 3 * UInt.SIZE_BYTES + 2 * UShort.SIZE_BYTES
    }
}