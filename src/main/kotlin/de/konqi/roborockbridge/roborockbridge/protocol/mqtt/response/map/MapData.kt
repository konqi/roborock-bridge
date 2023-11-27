package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

import org.springframework.security.crypto.codec.Hex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class MapData(val data: ByteArray) {
    val buffer = ByteBuffer.wrap(data).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN).mark()

    val preamble = String(ByteArray(2).apply { buffer.get(this) })

    val headerLength = buffer.getShort().toUShort()
    val bodyLength = buffer.getInt().toUInt()

    // wrap byte buffer around existing data to save memory
    val header = buffer.reset().slice().limit(headerLength.toInt()).order(ByteOrder.LITTLE_ENDIAN)
    val body = buffer.duplicate().position(headerLength.toInt()).slice().order(ByteOrder.LITTLE_ENDIAN)
    val digest = String(Hex.encode(data.copyOfRange(data.size - 20, data.size)))
    val calculatedDigest: String
        get() = String(
            Hex.encode(
                MessageDigest.getInstance("SHA-1").digest(data.copyOf(buffer.limit() - 20))
            )
        )


    val version = "${header.getShort(8).toUShort()}.${header.getShort(10).toUShort()}"
    val mapIndex = header.getInt(12).toUInt()
    val mapSequence = header.getInt(16).toUInt()

    init {
        if (preamble != "rr") {
            throw RuntimeException("Packet data does not seem to be map data. Incorrect preamble")
        }
        if (digest != calculatedDigest) {
            throw RuntimeException("Packet data corrupt. Mismatching digest")
        }
    }
}