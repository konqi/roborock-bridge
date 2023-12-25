package de.konqi.roborockbridge.remote.mqtt.response

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

abstract class MapDataWrapper<T>(
    val endpoint: String,
    val id: UShort,
    val payload: T
) {
    companion object {
        const val SCHEMA_TYPE = 301
    }
}

class Protocol301(binary: Protocol301Binary, key: ByteArray) : MapDataWrapper<MapDataPayload>(binary.endpoint, binary.id,
    payload = decryptAndDecode(binary.payload, key)
) {
    companion object {
        private const val CIPHER = "AES/CBC/PKCS5Padding"

        private fun decryptAndDecode(encrypted: ByteArray, key: ByteArray): MapDataPayload {
            val iv = IvParameterSpec(ByteArray(16) { 0 })
            val cipher = Cipher.getInstance(CIPHER).also {
                it.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(key, "AES"), iv
                )
            }

            val decryptedBytes = CipherInputStream(encrypted.inputStream(), cipher).use { inputStream ->
                GZIPInputStream(inputStream).readBytes()
            }

            return MapDataPayload.fromRawBytes(decryptedBytes)
        }
    }
}

class Protocol301Binary(endpoint: String, id: UShort, payload: ByteArray) : MapDataWrapper<ByteArray>(
    endpoint, id,
    payload
) {
    fun decryptAndDecode(key: ByteArray): Protocol301 {
        return Protocol301(this, key)
    }

    companion object {
        fun fromRawBytes(data: ByteArray): Protocol301Binary {
            val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val endpoint: String = String(ByteArray(15).also { buffer.get(it) }).trimEnd()

            // TODO: Determine if this byte belongs to the endpoint string
            // First impression: Looks like a weird string terminator
            val unknownNumber: UByte = buffer.get().toUByte()

            // TODO: Determine if this isn't actually UInt
            // First impression: likely - could even be a long, which would make sense to accommodate json numbers
            val id: UShort = buffer.getShort().toUShort()
            val unknownBytes = ByteArray(6).also { buffer.get(it) }
            val payload = ByteArray(buffer.remaining()).also { buffer.get(it) }

            return Protocol301Binary(endpoint = endpoint, id = id, payload = payload)
        }
    }
}
