package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.protocol.mqtt.response.map.Coordinate

data class MapDataForPublish(
    val map: String?,
    val robotPosition: Coordinate<Float>?,
    val chargerPosition: Coordinate<Float>?,
    val path: List<Coordinate<Float>>?,
    val gotoPath: List<Coordinate<Float>>?,
    val predictedPath: List<Coordinate<Float>>?
)