package de.konqi.roborockbridge

import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class UIntToByteArray {
    fun convert(number: UInt): ByteArray {
        return ByteBuffer.allocate(UInt.SIZE_BYTES).putInt(number.toInt()).array()
    }

    @Test
    fun testUIntToByteArrayConversion() {
        assert(convert(UInt.MAX_VALUE).contentEquals(ubyteArrayOf(0xffu, 0xffu, 0xffu, 0xffu).toByteArray()))
        assert(convert(UInt.MIN_VALUE).contentEquals(ubyteArrayOf(0x00u, 0x00u, 0x00u, 0x00u).toByteArray()))
        assert(convert(0x01u).contentEquals(ubyteArrayOf(0x00u, 0x00u, 0x00u, 0x01u).toByteArray()))
        assert(convert(0xffu).contentEquals(ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xffu).toByteArray()))
    }

    @Test
    fun testStringConversion() {
        val foo = "1.0"
        val byteArray = foo.toByteArray()
        assert(byteArray.size == 3)
        assert(byteArray.contentEquals(byteArrayOf(0x31,0x2E,0x30)))
    }
}