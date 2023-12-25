package de.konqi.roborockbridge.remote.mqtt.raw

import java.nio.ByteBuffer
import java.util.zip.CRC32

open class BinaryMessage(buffer: ByteBuffer = ByteBuffer.allocate(BinaryMessageHeader.HEADER_SIZE + BinaryMessageFooter.FOOTER_SIZE)) :
    BinaryProtocol {
    val header = BinaryMessageHeader(buffer.duplicate().position(0).limit(BinaryMessageHeader.HEADER_SIZE))
    private var body = BinaryMessageBody(
        buffer.duplicate().position(BinaryMessageHeader.HEADER_SIZE).slice().limit(header.payloadLength.toInt())
    )
    val footer = BinaryMessageFooter(
        buffer.duplicate().position(
            buffer.limit() - BinaryMessageFooter.FOOTER_SIZE
        ).slice()
    )

    open var payload: ByteArray
        get() = body.bytes
        set(value) {
            body = BinaryMessageBody(ByteBuffer.wrap(value))

            // update payload length
            header.payloadLength = value.size.toUShort()
        }

    val calculatedChecksum: UInt
        get() = CRC32().run {
            update(header.bytes)
            update(body.bytes)
            value.toUInt()
        }

    /**
     * Validates a read message checksum from message footer to calculated message
     * No useful during packet creation.
     */
    val valid: Boolean get() = footer.checksum == calculatedChecksum

    override val bytes: ByteArray
        get() = ByteBuffer.allocate(BinaryMessageHeader.HEADER_SIZE + body.size + BinaryMessageFooter.FOOTER_SIZE)
            .put(header.bytes)
            .put(body.bytes)
            .putInt(calculatedChecksum.toInt()).array()
}