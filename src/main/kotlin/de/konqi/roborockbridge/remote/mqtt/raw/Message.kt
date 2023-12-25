package de.konqi.roborockbridge.remote.mqtt.raw

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextUInt

open class Message : BinaryMessage {
    constructor(buffer: ByteBuffer): super(buffer)
    constructor(): super() {
        // set defaults for new message
        header.protocolVersion = "1.0"
        header.sequenceNumber = sequenceNumberCounter.incrementAndGet().toUInt()
        header.random = Random.nextUInt() % 2000u
    }

    companion object {
        private val sequenceNumberCounter = AtomicInteger()
    }
}

