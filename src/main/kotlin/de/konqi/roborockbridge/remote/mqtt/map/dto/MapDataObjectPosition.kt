package de.konqi.roborockbridge.remote.mqtt.map.dto

data class MapDataObjectPosition<T>(
    override val x: T,
    override val y: T,
    val a: Int? = null
) : Coordinate<T>(x, y) {
    constructor(coordinate: Coordinate<T>, a: Int?) : this(x = coordinate.x, y = coordinate.y, a = a)

    override fun toString(): String {
        return "${MapDataObjectPosition::class.simpleName}($x:$y,$a)"
    }

    companion object {
        fun fromRawMapDataSection(data: MapDataSection) = MapDataObjectPosition(
            x = data.body.getInt(0).toUInt(),
            y = data.body.getInt(UInt.SIZE_BYTES).toUInt(),
            a = if (data.bodyLength > UInt.SIZE_BYTES.toUInt() * 2u) data.body.getInt(2 * UInt.SIZE_BYTES) else null
        )
    }
}

fun MapDataObjectPosition<UInt>.correct(top: UInt, left: UInt) = MapDataObjectPosition(
    coordinate = (this as Coordinate<UInt>).correct(top, left),
    a = this.a
)

