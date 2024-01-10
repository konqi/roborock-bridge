package de.konqi.roborockbridge.bridge.dto

import de.konqi.roborockbridge.remote.mqtt.map.MapDataPayload
import de.konqi.roborockbridge.remote.mqtt.map.dto.Coordinate
import kotlin.reflect.full.declaredMemberProperties

data class MapDataForPublish(
    val map: String?,
    val bitmapData: String?,
    val robotPosition: Coordinate<Float>?,
    val chargerPosition: Coordinate<Float>?,
    val path: List<Coordinate<Float>>?,
    val gotoPath: List<Coordinate<Float>>?,
    val predictedPath: List<Coordinate<Float>>?,
    val virtualWalls: List<List<Coordinate<Float>>>
) {
    operator fun get(name: String): Any? {
        return fieldNames[name]?.invoke(this)
    }

    fun getFields() = fieldNames.keys

    companion object {
        val fieldNames = MapDataForPublish::class.declaredMemberProperties.associate { it.name to it.getter }

        fun fromProtocol301Payload(payload: MapDataPayload) = MapDataForPublish(
            map = payload.map?.getImageDataUrl(),
            bitmapData = payload.map?.getCompressedBitmapData(),
            robotPosition = payload.robotPosition,
            chargerPosition = payload.chargerPosition,
            gotoPath = payload.gotoPath?.points,
            path = payload.path?.points,
            predictedPath = payload.predictedPath?.points,
            virtualWalls = payload.virtualWalls?.zones?.map { listOf(it.start, it.end) } ?: listOf()
        )
    }
}