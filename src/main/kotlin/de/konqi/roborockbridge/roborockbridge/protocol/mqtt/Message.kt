package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextUInt

open class Message(raw: ByteArray? = null) : BinaryMessage(raw) {
    init {
        if(raw == null) {
            header.protocolVersion = "1.0"
            header.sequenceNumber = sequenceNumberCounter.incrementAndGet().toUInt()
            header.random = Random.nextUInt() % 2000u
        }
    }

    companion object {
        private val sequenceNumberCounter = AtomicInteger()
    }
}

