package de.konqi.roborockbridge.protocol.mqtt.response.map

internal fun parseRectangles(data: MapDataSection, count: Int) =
    List(count) { index ->
        mapOf(
            "x0" to data.body.getShort(index * 8 + 0).toUShort(),
            "y0" to data.body.getShort(index * 8 + 2).toUShort(),
            "x1" to data.body.getShort(index * 8 + 4).toUShort(),
            "y1" to data.body.getShort(index * 8 + 6).toUShort()
        )
    }