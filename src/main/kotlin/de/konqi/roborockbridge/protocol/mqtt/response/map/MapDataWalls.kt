package de.konqi.roborockbridge.protocol.mqtt.response.map

class MapDataWalls(data: MapDataSection) {
    val numberOfWalls = data.header.getShort(8).toUShort()
    val walls = parseRectangles(data, numberOfWalls.toInt())
}