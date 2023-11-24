package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.raw

import java.nio.ByteBuffer

class BinaryMessageBody(private val buffer: ByteBuffer = ByteBuffer.allocate(0)) : BinaryProtocol {
    val size: Int get() = buffer.limit()

    override val bytes: ByteArray
        get() = buffer.array().copyOfRange(buffer.arrayOffset(), buffer.arrayOffset() + buffer.limit())
}