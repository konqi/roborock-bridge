package de.konqi.roborockbridge.bridge.dto

import de.konqi.roborockbridge.remote.mqtt.map.MapDataPayload
import de.konqi.roborockbridge.remote.mqtt.map.dto.Coordinate
import de.konqi.roborockbridge.utility.DataCompressor
import de.konqi.roborockbridge.utility.Meta
import de.konqi.roborockbridge.utility.base64
import de.konqi.roborockbridge.utility.deflate
import kotlin.reflect.full.declaredMemberProperties

data class MapDataForPublish(
    val map: DataCompressor<String>?,
    val bitmapData: DataCompressor<String>?,
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
            map = if (payload.map != null) DataCompressor(
                data = payload.map.getImageBytes(),
                meta = Meta(
                    mimeType = "image/png",
                    dimensions = listOf(payload.map.width.toInt(), payload.map.height.toInt())
                )
            ).base64() else null,
            bitmapData = payload.map?.getCompressedBitmapData()?.deflate()?.base64(),
            robotPosition = payload.robotPosition,
            chargerPosition = payload.chargerPosition,
            gotoPath = payload.gotoPath?.points,
            path = payload.path?.points,
            predictedPath = payload.predictedPath?.points,
            virtualWalls = payload.virtualWalls?.zones?.map { listOf(it.start, it.end) } ?: listOf()
        )
    }
}