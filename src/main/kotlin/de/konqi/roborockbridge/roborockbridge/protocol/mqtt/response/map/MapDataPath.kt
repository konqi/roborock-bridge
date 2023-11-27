package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

class MapDataPath(data: MapDataSection) {
    val end = data.header.getInt(4).toUInt()
    val numberOfWaypoints = data.header.getInt(8).toUInt()
    val size = data.header.getInt(12).toUInt()
    val angle = data.header.getInt(16).toUInt()
    val points = List(numberOfWaypoints.toInt()) { index ->
        data.body.getShort(index * 4).toUShort() to data.body.getShort(index * 4 + 2).toUShort()
    }
}