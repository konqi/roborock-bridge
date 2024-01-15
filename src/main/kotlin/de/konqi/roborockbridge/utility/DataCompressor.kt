package de.konqi.roborockbridge.utility

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

data class Meta(
    val mimeType: String,
    val dimensions: List<Int> = listOf() // i.e. [<width>,<height>]
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = ["meta", "mode", "data"])
data class DataCompressor<T>(
    val data: T,
    val mode: List<String> = listOf(), // i.e. ["deflate", "base64"]
    val meta: Meta? = null
)

inline fun <reified T> DataCompressor<ByteArray>.streamCompress(inputStreamType: Class<T>): ByteArray where T : InputStream {
    return ByteArrayOutputStream().use {
        inputStreamType
            .getConstructor(InputStream::class.java)
            .newInstance(data.inputStream())
            .transferTo(it)

        it.toByteArray()
    }
}

fun DataCompressor<ByteArray>.deflate(): DataCompressor<ByteArray> {
    return this.copy(
        data = streamCompress(DeflaterInputStream::class.java),
        mode = this.mode + "deflate"
    )
}

fun DataCompressor<ByteArray>.gzip(): DataCompressor<ByteArray> {
    return this.copy(
        data = streamCompress(GZIPInputStream::class.java),
        mode = this.mode + "gzip"
    )
}

fun DataCompressor<ByteArray>.base64(): DataCompressor<String> {
    return DataCompressor(
        data = String(Base64.getEncoder().encode(this.data)),
        mode = this.mode + "base64",
        meta = meta
    )
}