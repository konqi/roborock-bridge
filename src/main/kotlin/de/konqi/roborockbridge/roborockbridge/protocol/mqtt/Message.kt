package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.util.concurrent.atomic.AtomicInteger

open class Message(raw: ByteArray? = null) : BinaryMessage(raw) {
    init {
        header.protocolVersion = "1.0"
        header.sequenceNumber = sequenceNumberCounter.incrementAndGet().toUInt()
    }

    companion object {
        private val sequenceNumberCounter = AtomicInteger()
    }
}

