package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

class MapDataObjectPosition(data: MapDataSection) {
    val x = data.body.getInt(0).toUInt()
    val y = data.body.getInt(UInt.SIZE_BYTES).toUInt()
    val a = if (data.bodyLength > UInt.SIZE_BYTES.toUInt() * 2u) data.body.getInt(2 * UInt.SIZE_BYTES) else null
}