package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

class MapDataZones(data: MapDataSection) {
    val numberOfZones = data.header.getShort(8).toUShort()
    val zones = parseRectangles(data, numberOfZones.toInt())
}