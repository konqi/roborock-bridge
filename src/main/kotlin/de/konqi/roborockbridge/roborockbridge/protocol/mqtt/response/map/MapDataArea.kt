package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

class MapDataArea(data: MapDataSection) {
    val numberOfAreas = data.header.getShort(8).toUShort()
    val areas = List(numberOfAreas.toInt()) { index ->
        mapOf(
            "x0" to data.body.getShort(index * 8 + 0).toUShort(),
            "y0" to data.body.getShort(index * 8 + 2).toUShort(),
            "x1" to data.body.getShort(index * 8 + 4).toUShort(),
            "y1" to data.body.getShort(index * 8 + 6).toUShort(),
            "x2" to data.body.getShort(index * 8 + 8).toUShort(),
            "y2" to data.body.getShort(index * 8 + 10).toUShort(),
            "x3" to data.body.getShort(index * 8 + 12).toUShort(),
            "y3" to data.body.getShort(index * 8 + 14).toUShort()
        )
    }
}