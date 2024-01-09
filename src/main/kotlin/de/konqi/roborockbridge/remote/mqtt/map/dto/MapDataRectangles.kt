package de.konqi.roborockbridge.remote.mqtt.map.dto

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MapDataRectangle<T>(val start: Coordinate<T>, val end: Coordinate<T>) {
    companion object {
        fun fromRaw(buffer: ByteBuffer) =
            MapDataRectangle(
                start = Coordinate(x = buffer.getShort(0).toUShort(), y = buffer.getShort(2).toUShort()),
                end = Coordinate(x = buffer.getShort(4).toUShort(), y = buffer.getShort(6).toUShort())
            )

        fun fromRawTimes(buffer: ByteBuffer, count: Int) =
            List(count) { index -> fromRaw(buffer.slice(index * 8, 8).order(ByteOrder.LITTLE_ENDIAN)) }
    }
}

fun MapDataRectangle<UShort>.correct(top: UShort, left: UShort) = MapDataRectangle(
    start = start.correct(top, left), end = end.correct(top, left)
)

data class MapDataRectangles<T>(val numberOfZones: UShort, val zones: List<MapDataRectangle<T>>) {
    companion object {
        fun fromMapDataSection(data: MapDataSection): MapDataRectangles<UShort> {
            val numberOfZones = data.header.getShort(8).toUShort()

            return MapDataRectangles(
                numberOfZones = numberOfZones,
                zones = MapDataRectangle.fromRawTimes(data.body, numberOfZones.toInt())
            )
        }
    }
}

fun MapDataRectangles<UShort>.correct(top: UShort, left: UShort) = MapDataRectangles(
    numberOfZones = this.numberOfZones,
    zones = zones.map {
        it.correct(top, left)
    }
)