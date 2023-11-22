package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.raw

import java.nio.ByteBuffer
import java.util.zip.CRC32

open class BinaryMessage(input: ByteArray? = null) : BinaryProtocol {
    val header = BinaryMessageHeader(ByteBuffer.allocate(BinaryMessageHeader.HEADER_SIZE).also {
        if (input != null) {
            it.put(input, 0, BinaryMessageHeader.HEADER_SIZE)
        }
    })
    private var payloadBuffer = if (input != null) {
        ByteBuffer.allocate(header.payloadLength.toInt()).also {
            it.put(
                input,
                BinaryMessageHeader.HEADER_SIZE,
                input.size - BinaryMessageHeader.HEADER_SIZE - UInt.SIZE_BYTES
            )
        }
    } else ByteBuffer.allocate(0)
    private val checksumBuffer = ByteBuffer.allocate(UInt.SIZE_BYTES).also {
        if (input != null) {
            it.put(input, input.size - UInt.SIZE_BYTES, UInt.SIZE_BYTES)
        }
    }

    open var payload: ByteArray
        get() = payloadBuffer.array()
        set(value) {
            payloadBuffer = ByteBuffer.wrap(value)

            // update payload length
            header.payloadLength = value.size.toUShort()
        }

    val checksum: UInt get() = checksumBuffer.getInt(0).toUInt()

    val calculatedChecksum: UInt
        get() = CRC32().run {
            update(header.bytes)
            update(payloadBuffer.array())
            value.toUInt()
        }

    val valid: Boolean get() = checksum == calculatedChecksum

    override val bytes: ByteArray
        get() = ByteBuffer.allocate(BinaryMessageHeader.HEADER_SIZE + payloadBuffer.capacity() + checksumBuffer.capacity())
            .put(header.bytes)
            .put(payloadBuffer)
            .putInt(calculatedChecksum.toInt()).array()
}