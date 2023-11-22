package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Response301(data: ByteArray) {
    val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    val endpoint: String = String(ByteArray(15).also { buffer.get(it) }).trimEnd()
    val unknownNumber: UByte = buffer.get().toUByte()
    val id: UShort = buffer.getShort().toUShort()
    val unknownBytes = ByteArray(6).also { buffer.get(it)}
    val payload = ByteArray(buffer.remaining()).also { buffer.get(it) }

    fun decrypt(key: ByteArray): ByteArray {
        val iv = IvParameterSpec(ByteArray(16) { 0 })
        val cipher = Cipher.getInstance(CIPHER).also {
            it.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"), iv
            )
        }

        val decrypted = CipherInputStream(payload.inputStream(), cipher).use {
            GZIPInputStream(it).readBytes()
        }

        return decrypted
    }

    companion object {
        const val CIPHER = "AES/CBC/PKCS5Padding"
    }
}