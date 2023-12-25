package de.konqi.roborockbridge.remote.mqtt.response.map

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

data class Room(
    var xMin: Int,
    var xMax: Int,
    var yMin: Int,
    var yMax: Int
)

enum class PixelType {
    OUTSIDE,
    INSIDE,
    WALL,
    SCAN,
    OBSTACLE_WALL,
    OBSTACLE_WALL_V2,
    OBSTACLE_ROOM_0,
    OBSTACLE_ROOM_1,
    OBSTACLE_ROOM_2,
    OBSTACLE_ROOM_3,
    OBSTACLE_ROOM_4,
    OBSTACLE_ROOM_5,
    OBSTACLE_ROOM_6,
    OBSTACLE_ROOM_7,
    OBSTACLE_ROOM_8,
    OBSTACLE_ROOM_9,
    OBSTACLE_ROOM_10,
    OBSTACLE_ROOM_11,
    OBSTACLE_ROOM_12,
    OBSTACLE_ROOM_13,
    OBSTACLE_ROOM_14,
    OBSTACLE_ROOM_15,
    OTHER
}

class MapDataImage(data: MapDataSection) {
    val top = data.header.getInt(data.header.limit() - 16).toUInt()
    val left = data.header.getInt(data.header.limit() - 12).toUInt()
    val height = data.header.getInt(data.header.limit() - 8).toUInt()
    val width = data.header.getInt(data.header.limit() - 4).toUInt()
    val rooms = HashMap<Int, Room>()
    private val bitmap = Array(height.toInt()) { y ->
        Array(width.toInt()) { x ->
            when (val pixel = data.body.get(width.toInt() * y + x)) {
                OUTSIDE -> PixelType.OUTSIDE
                WALL -> PixelType.WALL
                INSIDE -> PixelType.INSIDE
                SCAN -> PixelType.SCAN
                else -> when (pixel and BITMASK_OBSTACLE) {
                    OBSTACLE_WALL -> PixelType.OBSTACLE_WALL
                    OBSTACLE_WALL_V2 -> PixelType.OBSTACLE_WALL_V2
                    OBSTACLE_ROOM -> {
                        // other implementations shr by 3 making the lowest number 16
                        // since the first bit is always 1, working with 2^4 ids zero-based is easier
                        val roomNumber = (pixel.toInt() and 0xFF) shr 3 and 0xF

                        val room = rooms[roomNumber]
                        if (room == null) {
                            rooms[roomNumber] = Room(xMin = x, xMax = x, yMin = y, yMax = y)
                        } else {
                            room.xMin = min(room.xMin, x)
                            room.yMin = min(room.yMin, y)
                            room.xMax = max(room.xMax, x)
                            room.yMax = max(room.yMax, y)
                        }

                        PixelType.valueOf("OBSTACLE_ROOM_$roomNumber")
                    }

                    else -> PixelType.OTHER
                }
            }
        }
    }

    private fun getImageProjection(palette: Map<PixelType, Color> = DEFAULT_PALETTE): BufferedImage {
        return BufferedImage(width.toInt(), height.toInt(), BufferedImage.TRANSLUCENT).also { img ->
            bitmap.reversed().forEachIndexed { y, row ->
                row.forEachIndexed { x, pix ->
                    if (palette.containsKey(pix)) {
                        img.setRGB(x, y, palette[pix]!!.rgb)
                    }
                }
            }
        }
    }

    fun getImageBytes(palette: Map<PixelType, Color> = DEFAULT_PALETTE, formatName: String = "png"): ByteArray {
        return ByteArrayOutputStream().use { os ->
            ImageIO.write(
                getImageProjection(palette),
                formatName,
                os
            )
            os.toByteArray()
        }
    }

    fun getImageDataUrl(palette: Map<PixelType, Color> = DEFAULT_PALETTE, formatName: String = "png"): String {
        return "data:image/$formatName;base64,${Base64.getEncoder().encodeToString(getImageBytes(palette, formatName))}"
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

        val RR_ORANGE = Color(255, 148, 120)
        val RR_YELLOW = Color(255, 207, 78)
        val RR_GREEN = Color(43, 205, 187)
        val RR_BLUE = Color(130, 190, 255)
        val RR_GRAY = Color(109, 110, 112)

        val DEFAULT_PALETTE: Map<PixelType, Color> = mapOf(
            PixelType.OBSTACLE_WALL_V2 to RR_GRAY,
            PixelType.OBSTACLE_ROOM_0 to RR_BLUE,
            PixelType.OBSTACLE_ROOM_1 to RR_ORANGE,
            PixelType.OBSTACLE_ROOM_2 to RR_GREEN,
            PixelType.OBSTACLE_ROOM_3 to RR_YELLOW,
            PixelType.OBSTACLE_ROOM_4 to RR_BLUE,
            PixelType.OBSTACLE_ROOM_5 to RR_ORANGE,
            PixelType.OBSTACLE_ROOM_6 to RR_GREEN,
            PixelType.OBSTACLE_ROOM_7 to RR_YELLOW,
            PixelType.OBSTACLE_ROOM_8 to RR_BLUE,
            PixelType.OBSTACLE_ROOM_9 to RR_ORANGE,
            PixelType.OBSTACLE_ROOM_10 to RR_GREEN,
            PixelType.OBSTACLE_ROOM_11 to RR_YELLOW,
            PixelType.OBSTACLE_ROOM_12 to RR_BLUE,
            PixelType.OBSTACLE_ROOM_13 to RR_ORANGE,
            PixelType.OBSTACLE_ROOM_14 to RR_GREEN,
            PixelType.OBSTACLE_ROOM_15 to RR_YELLOW,
        )
    }
}