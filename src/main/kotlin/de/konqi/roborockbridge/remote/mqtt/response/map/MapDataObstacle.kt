package de.konqi.roborockbridge.remote.mqtt.response.map

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Obstacle<T>(
    override val x: T,
    override val y: T,
    val type: ObstacleType? = null,
    val u1: Short? = null,
    val u2: Short? = null,
    val photo: String? = null
) : Coordinate<T>(x, y) {
    constructor(
        coordinate: Coordinate<T>,
        type: ObstacleType? = null,
        u1: Short? = null,
        u2: Short? = null,
        photo: String? = null
    ) : this(x = coordinate.x, y = coordinate.y, type = type, u1 = u1, u2 = u2, photo = photo)

    companion object {
        private fun fromRawMin(buffer: ByteBuffer): Obstacle<UShort> {
            val x = buffer.getShort(0).toUShort()
            val y = buffer.getShort(2).toUShort()

            return Obstacle(x = x, y = y)
        }

        private fun fromRawWithType(buffer: ByteBuffer): Obstacle<UShort> {
            return fromRawMin(buffer)
                .copy(
                    type = ObstacleType.fromValue(buffer.getShort(4).toUShort())
                )
        }

        private fun fromRawWithU(buffer: ByteBuffer): Obstacle<UShort> {
            return fromRawWithType(buffer)
                .copy(
                    u1 = buffer.getShort(6),
                    u2 = buffer.getShort(8)
                )
        }

        private fun fromRawWithPhoto(buffer: ByteBuffer): Obstacle<UShort> {
            return fromRawWithU(buffer)
                .copy(
                    // TODO: Investigate the Short?-value at offset 10. It's not the length, but could be part of the string
                    photo = charset("ascii").decode(buffer.duplicate().position(12).slice().limit(16)).toString()
                )
        }

        fun fromRaw(buffer: ByteBuffer, size: UInt): Obstacle<UShort> {
            return if (size == 28u) {
                fromRawWithPhoto(buffer)
            } else if (size >= 10u) {
                fromRawWithU(buffer)
            } else if (size >= 6u) {
                fromRawWithType(buffer)
            } else {
                fromRawMin(buffer)
            }
        }
    }
}

fun Obstacle<UShort>.correct(top: UShort, left: UShort): Obstacle<Float> = Obstacle(
    coordinate = (this as Coordinate<UShort>).correct(top, left),
    type = type,
    u1 = u1,
    u2 = u2,
    photo = photo
)


data class MapDataObstacle<T>(val numberOfObstacles: UShort, val obstacles: List<Obstacle<T>>) {
    companion object {
        fun fromRawMapDataSection(data: MapDataSection): MapDataObstacle<UShort> {
            val numberOfObstacles = data.header.getShort(8).toUShort()
            val obstacles =
                if (numberOfObstacles > 0u) (data.bodyLength / numberOfObstacles).let { sizeOfObstacleInBytes ->
                    List(numberOfObstacles.toInt()) { index ->
                        Obstacle.fromRaw(
                            buffer = data.body.slice(
                                index * sizeOfObstacleInBytes.toInt(),
                                sizeOfObstacleInBytes.toInt()
                            ).order(
                                ByteOrder.LITTLE_ENDIAN
                            ),
                            size = sizeOfObstacleInBytes
                        )
                    }
                } else emptyList()

            return MapDataObstacle(
                numberOfObstacles = numberOfObstacles,
                obstacles = obstacles
            )
        }
    }
}