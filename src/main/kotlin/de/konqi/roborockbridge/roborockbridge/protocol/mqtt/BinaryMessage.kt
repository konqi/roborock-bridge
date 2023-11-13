package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.nio.ByteBuffer
import java.util.zip.CRC32

open class BinaryMessage(input: ByteArray? = null) : BinaryProtocol {
    val header = if(input != null) BinaryMessageHeader(
        ByteBuffer.wrap(
            input,
            0,
            BinaryMessageHeader.HEADER_SIZE
        )
    ) else BinaryMessageHeader()
    private var payloadBuffer = if(input != null) ByteBuffer.wrap(
        input,
        BinaryMessageHeader.HEADER_SIZE,
        header.payloadLength.toInt()
    ) else ByteBuffer.allocate(0)
    private val checksumBuffer = if(input != null) ByteBuffer.wrap(
        input,
        input.size - Long.SIZE_BYTES,
        Long.SIZE_BYTES
    ) else ByteBuffer.allocate(Long.SIZE_BYTES)

    open var payload: ByteArray
        get() = payloadBuffer.array().copyOfRange(payloadBuffer.position(), payloadBuffer.limit())
        set(value) {
            payloadBuffer = ByteBuffer.wrap(value)

            // update payload length
            header.payloadLength = value.size.toUShort()
        }

    val checksum: Long get() = checksumBuffer.getLong(0)

    val calculatedChecksum: Long
        get() = CRC32().run {
            update(header.bytes)
            update(payloadBuffer.array())
            value
        }

    override val bytes: ByteArray
        get() = ByteBuffer.allocate(BinaryMessageHeader.HEADER_SIZE + payloadBuffer.capacity() + checksumBuffer.capacity())
            .put(header.bytes)
            .put(payloadBuffer)
            .putLong(calculatedChecksum).array()
}