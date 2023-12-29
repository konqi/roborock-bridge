package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.remote.mqtt.response.MapDataPayload
import de.konqi.roborockbridge.remote.mqtt.response.Protocol301
import de.konqi.roborockbridge.remote.mqtt.response.map.Coordinate
import kotlin.reflect.full.declaredMemberProperties

data class MapDataForPublish(
    val map: String?,
    val robotPosition: Coordinate<Float>?,
    val chargerPosition: Coordinate<Float>?,
    val path: List<Coordinate<Float>>?,
    val gotoPath: List<Coordinate<Float>>?,
    val predictedPath: List<Coordinate<Float>>?,
    val virtualWalls: List<Pair<Coordinate<Float>, Coordinate<Float>>>
) {
    operator fun get(name: String): Any? {
        return fieldNames[name]?.invoke(this)
    }

    fun getFields() = fieldNames.keys

    companion object {
        val fieldNames = MapDataForPublish::class.declaredMemberProperties.associate { it.name to it.getter }

        fun fromProtocol301Payload(payload: MapDataPayload) = MapDataForPublish(
            map = payload.map?.getImageDataUrl(),
            robotPosition = payload.robotPosition,
            chargerPosition = payload.chargerPosition,
            gotoPath = payload.gotoPath?.points,
            path = payload.path?.points,
            predictedPath = payload.predictedPath?.points,
            virtualWalls = payload.virtualWalls?.zones?.map { it.start to it.end } ?: listOf()
        )
    }
}