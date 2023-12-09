package de.konqi.roborockbridge.protocol.mqtt.raw

import java.nio.ByteBuffer

class BinaryMessageFooter(private val buffer: ByteBuffer = ByteBuffer.allocate(FOOTER_SIZE)) : BinaryProtocol {
    var checksum: UInt
        get() = buffer.getInt(0).toUInt()
        set(value) {
            buffer.position(0).putInt(value.toInt())
        }

    override val bytes: ByteArray
        get() = buffer.array().copyOfRange(buffer.arrayOffset(), buffer.arrayOffset() + buffer.limit())

    companion object {
        const val FOOTER_SIZE = UInt.SIZE_BYTES
    }
}