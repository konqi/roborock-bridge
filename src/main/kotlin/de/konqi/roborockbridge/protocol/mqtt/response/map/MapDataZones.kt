package de.konqi.roborockbridge.protocol.mqtt.response.map

class MapDataZones(data: MapDataSection) {
    val numberOfZones = data.header.getShort(8).toUShort()
    val zones = parseRectangles(data, numberOfZones.toInt())
}