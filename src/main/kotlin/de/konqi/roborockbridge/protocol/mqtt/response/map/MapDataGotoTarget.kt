package de.konqi.roborockbridge.protocol.mqtt.response.map

data class MapDataGotoTarget<T>(override val x: T, override val y: T) : Coordinate<T>(
    x,
    y
) {
    constructor(coordinate: Coordinate<T>) : this(x = coordinate.x, y = coordinate.y)

    companion object {
        fun fromMapDataSection(data: MapDataSection) = MapDataGotoTarget(
            x = data.body.getShort(0).toUShort(),
            y = data.body.getShort(UShort.SIZE_BYTES).toUShort()
        )
    }
}

fun MapDataGotoTarget<UShort>.correct(top: UShort, left: UShort) = MapDataGotoTarget(
    coordinate = (this as Coordinate<UShort>).correct(top, left)
)
