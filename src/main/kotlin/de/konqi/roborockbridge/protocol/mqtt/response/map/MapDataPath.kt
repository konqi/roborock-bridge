package de.konqi.roborockbridge.protocol.mqtt.response.map

data class MapDataPath<T>(
    val end: UInt,
    val numberOfWaypoints: UInt,
    val size: UInt,
    val angle: UInt,
    val points: List<Coordinate<T>>
) {
    companion object {
        fun fromRawMapDataSection(data: MapDataSection): MapDataPath<UShort> {
            val numberOfWaypoints = data.header.getInt(8).toUInt()

            return MapDataPath(
                end = data.header.getInt(4).toUInt(),
                numberOfWaypoints = numberOfWaypoints,
                size = data.header.getInt(12).toUInt(),
                angle = data.header.getInt(16).toUInt(),
                points = List(numberOfWaypoints.toInt()) { index ->
                    Coordinate(
                        x = data.body.getShort(index * 4).toUShort(),
                        y = data.body.getShort(index * 4 + 2).toUShort()
                    )
                }
            )
        }
    }
}

fun MapDataPath<UShort>.correct(top: UShort, left: UShort) = MapDataPath(
    end = this.end,
    numberOfWaypoints = this.numberOfWaypoints,
    size = this.size,
    angle = this.angle,
    points = points.map {
        it.correct(top, left)
    }
)

