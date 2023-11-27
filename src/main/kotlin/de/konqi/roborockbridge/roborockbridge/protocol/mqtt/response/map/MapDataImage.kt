package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

data class Room(
    var xMin: Int,
    var xMax: Int,
    var yMin: Int,
    var yMax: Int
)

class MapDataImage(data: MapDataSection) {
    val top = data.header.getInt(data.header.limit() - 16).toUInt()
    val left = data.header.getInt(data.header.limit() - 12).toUInt()
    val height = data.header.getInt(data.header.limit() - 8).toUInt()
    val width = data.header.getInt(data.header.limit() - 4).toUInt()
    val rooms = HashMap<Int, Room>()

    init {
        val image = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TRANSLUCENT)
        for (y in 0..<height.toInt()) {
            val yOffset = width.toInt() * y
            for (x in 0..<width.toInt()) {
                when (val pixel: Byte = data.body.get(yOffset + x)) {
                    OUTSIDE -> {
                        image.setRGB(x, y, Color.TRANSLUCENT)
                    }

                    WALL -> {
                        image.setRGB(x, y, Color.BLACK.rgb)
                    }

                    INSIDE -> {
                        image.setRGB(x, y, Color.BLUE.rgb)
                    }

                    SCAN -> {
                        image.setRGB(x, y, Color.GREEN.rgb)
                    }

                    else -> {
                        when (pixel and BITMASK_OBSTACLE) {
                            OBSTACLE_WALL -> {
                                image.setRGB(x, y, Color.GRAY.rgb)
                            }

                            OBSTACLE_WALL_V2 -> {
                                image.setRGB(x, y, Color.MAGENTA.rgb)
                            }

                            OBSTACLE_ROOM -> {
                                val roomNumber = (pixel.toInt() and 0xFF) shr 3
                                image.setRGB(x, y, Color.YELLOW.rgb)

                                val room = rooms[roomNumber]
                                if (room == null) {
                                    rooms[roomNumber] = Room(xMin = x, xMax = x, yMin = y, yMax = y)
                                } else {
                                    room.xMin = min(room.xMin, x)
                                    room.yMin = min(room.yMin, y)
                                    room.xMax = max(room.xMax, x)
                                    room.yMax = max(room.yMax, y)
                                }
                            }

                            else -> {
                                image.setRGB(x, y, Color.ORANGE.rgb)
                            }
                        }
                    }
                }
            }
        }

        image.flush()
//        FileOutputStream("bild0.png").use {
//            ImageIO.write(image, "png", it)
//            println("wrote image")
//        }
    }

    companion object {
        const val OUTSIDE: Byte = 0x00
        const val WALL: Byte = 0x01
        const val INSIDE: Byte = 0xFF.toByte()
        const val SCAN: Byte = 0x07

        const val BITMASK_OBSTACLE: Byte = 0x07

        const val OBSTACLE_WALL = 0x00.toByte()
        const val OBSTACLE_WALL_V2 = 0x01.toByte()
        const val OBSTACLE_ROOM = 0x07.toByte()
    }
}