package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

class MapDataGotoTarget(data: MapDataSection) {
    val x = data.body.getShort(0).toUShort()
    val y = data.body.getShort(UShort.SIZE_BYTES).toUShort()
}