package de.konqi.roborockbridge.protocol.mqtt.response.map

class MapDataObjectPosition(data: MapDataSection) {
    val x: UInt = data.body.getInt(0).toUInt()
    val y: UInt = data.body.getInt(UInt.SIZE_BYTES).toUInt()
    val a: Int? = if (data.bodyLength > UInt.SIZE_BYTES.toUInt() * 2u) data.body.getInt(2 * UInt.SIZE_BYTES) else null

    override fun toString(): String {
        return "${MapDataObjectPosition::class.simpleName}($x:$y,$a)"
    }
}