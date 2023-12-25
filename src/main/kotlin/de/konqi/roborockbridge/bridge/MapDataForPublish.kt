package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.remote.mqtt.response.map.Coordinate
import kotlin.reflect.full.declaredMemberProperties

data class MapDataForPublish(
    val map: String?,
    val robotPosition: Coordinate<Float>?,
    val chargerPosition: Coordinate<Float>?,
    val path: List<Coordinate<Float>>?,
    val gotoPath: List<Coordinate<Float>>?,
    val predictedPath: List<Coordinate<Float>>?
) {
    operator fun get(name: String): Any? {
        return fieldNames[name]?.invoke(this)
    }

    fun getFields() = fieldNames.keys

    companion object {
        val fieldNames = MapDataForPublish::class.declaredMemberProperties.associate { it.name to it.getter }
    }
}