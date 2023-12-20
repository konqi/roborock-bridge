package de.konqi.roborockbridge.protocol.mqtt.response.map

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MapDataAreaVertices<T>(
    val first: Coordinate<T>,
    val second: Coordinate<T>,
    val third: Coordinate<T>,
    val forth: Coordinate<T>,
) {
    companion object {
        fun fromRaw(buffer: ByteBuffer) = MapDataAreaVertices(
            first = Coordinate(
                x = buffer.getShort(0).toUShort(), y = buffer.getShort(2).toUShort()
            ),
            second = Coordinate(
                x = buffer.getShort(4).toUShort(), y = buffer.getShort(6).toUShort()
            ),
            third = Coordinate(
                x = buffer.getShort(8).toUShort(), y = buffer.getShort(10).toUShort()
            ),
            forth = Coordinate(
                x = buffer.getShort(12).toUShort(), y = buffer.getShort(14).toUShort()
            )
        )
    }
}

fun MapDataAreaVertices<UShort>.correct(top: UShort, left: UShort) = MapDataAreaVertices(
    first = this.first.correct(top, left),
    second = this.second.correct(top, left),
    third = this.third.correct(top, left),
    forth = this.forth.correct(top, left)
)

data class MapDataArea<T>(val numberOfAreas: UShort, val areas: List<MapDataAreaVertices<T>>) {
    companion object {
        fun fromMapDataSection(data: MapDataSection): MapDataArea<UShort> {
            val numberOfAreas = data.header.getShort(8).toUShort()
            val areas = List(numberOfAreas.toInt()) { index ->
                MapDataAreaVertices.fromRaw(data.body.slice(index * 8, 8).order(ByteOrder.LITTLE_ENDIAN))
            }

            return MapDataArea(numberOfAreas = numberOfAreas, areas = areas)
        }
    }
}

fun MapDataArea<UShort>.correct(top: UShort, left: UShort) =
    MapDataArea(numberOfAreas = this.numberOfAreas, areas = this.areas.map { it.correct(top, left) })
