package de.konqi.roborockbridge.remote.mqtt.response.map

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.Deflater
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
    OBSTACLE_ROOM_16,
    OBSTACLE_ROOM_17,
    OBSTACLE_ROOM_18,
    OBSTACLE_ROOM_19,
    OBSTACLE_ROOM_20,
    OBSTACLE_ROOM_21,
    OBSTACLE_ROOM_22,
    OBSTACLE_ROOM_23,
    OBSTACLE_ROOM_24,
    OBSTACLE_ROOM_25,
    OBSTACLE_ROOM_26,
    OBSTACLE_ROOM_27,
    OBSTACLE_ROOM_28,
    OBSTACLE_ROOM_29,
    OBSTACLE_ROOM_30,
    OBSTACLE_ROOM_31,
    OBSTACLE_ROOM_0,
    INSIDE,
    WALL,
    SCAN,
    OBSTACLE_WALL,
    OBSTACLE_WALL_V2,
    OTHER;

    companion object {
        val ROOMS = listOf(
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
            OBSTACLE_ROOM_15
        )
    }
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
                        // use bit 4 to 8 for room number
                        // the first bit is always 1, making the lowest number 16
                        val roomNumber = (pixel.toInt() and 0xFF) shr 3

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

    fun getCompressedBitmapData(): String {
        val bytes = bitmap.flatMap { it.map { it.ordinal.toByte() } }.toByteArray()
        val output = ByteBuffer.allocate(bytes.size).also { buffer ->
            Deflater().run {
                setInput(bytes)
                finish()
                deflate(buffer)
                end()
            }
        }.flip()

        val compressedBytes = ByteArray(output.limit()).also {
            output.get(it)
        }

        return "data:deflated-bitmap/${width}x${height};base64,${Base64.getEncoder().encodeToString(compressedBytes)}"
    }

    /**
     * Determines connections between rooms
     *
     * <strong>Note:</strong> It is difficult determine the location of the adjoining rooms (e.g. above, left)
     * because some pixels are wildly off (mirrors, glass, etc.), also this information is not strictly necessary
     * to draw a map where two adjoining rooms don't use the same color
     */
    private fun detectRoomConnections(): Map<PixelType, MutableSet<PixelType>> {
        val roomConnections = PixelType.ROOMS.associateWith { mutableSetOf<PixelType>() }

        val putIfConnected = { otherPixelType: PixelType,
                               pixelType: PixelType ->
            if (PixelType.ROOMS.contains(otherPixelType) && pixelType != otherPixelType) {
                roomConnections[pixelType]!!.add(otherPixelType)
            }
        }

        bitmap.forEachIndexed { y, row ->
            row.forEachIndexed { x, pixelType ->
                if (PixelType.ROOMS.contains(pixelType)) {
                    if (x > 0) {
                        val otherPixelType = bitmap[y][x - 1]
                        putIfConnected(otherPixelType, pixelType)
                    }
                    if (y > 0) {
                        val otherPixelType = bitmap[y - 1][x]
                        putIfConnected(otherPixelType, pixelType)
                    }
                }
            }
        }

        // make connections bi-directional
        roomConnections.forEach {
            it.value.forEach { other -> roomConnections[other]!!.add(it.key) }
        }

        return roomConnections
    }

    private fun createDynamicPalette(roomColors: List<Color> = DEFAULT_ROOM_PALETTE): Map<PixelType, Color> {
        val dynamicPalette: MutableMap<PixelType, Color> = mutableMapOf()

        val roomConnections = detectRoomConnections()
        roomConnections.forEach { (currentRoomId, connectedRooms) ->
            // find colors that are not in use by connected rooms
            val colorsUsedByConnectedRooms = connectedRooms.mapNotNull { dynamicPalette[it] }
            val possibleColors = roomColors.filter { it !in colorsUsedByConnectedRooms }
            // when all colors are taken then there are only bad choices (also congrats to your home)
            dynamicPalette[currentRoomId] = if (possibleColors.isNotEmpty()) {
                possibleColors[currentRoomId.ordinal % possibleColors.size]
            } else {
                roomColors[currentRoomId.ordinal % roomColors.size]
            }
        }

        return dynamicPalette
    }

    private fun getImageProjection(
        wallColor: Color = RR_GRAY,
        roomColors: List<Color> = DEFAULT_ROOM_PALETTE
    ): BufferedImage {
        val palette = mapOf(PixelType.OBSTACLE_WALL_V2 to wallColor).plus(createDynamicPalette(roomColors))

        return BufferedImage(width.toInt(), height.toInt(), BufferedImage.TRANSLUCENT).also { img ->
            bitmap.forEachIndexed { y, row ->
                row.forEachIndexed { x, pix ->
                    if (palette.containsKey(pix)) {
                        img.setRGB(x, y, palette[pix]!!.rgb)
                    }
                }
            }
        }
    }

    fun getImageBytes(
        wallColor: Color = RR_GRAY,
        roomColors: List<Color> = DEFAULT_ROOM_PALETTE,
        formatName: String = "png"
    ): ByteArray {
        return ByteArrayOutputStream().use { os ->
            ImageIO.write(
                getImageProjection(wallColor, roomColors),
                formatName,
                os
            )
            os.toByteArray()
        }
    }

    fun getImageDataUrl(
        wallColor: Color = RR_GRAY,
        roomColors: List<Color> = DEFAULT_ROOM_PALETTE,
        formatName: String = "png"
    ): String {
        return "data:image/$formatName;base64,${
            Base64.getEncoder().encodeToString(getImageBytes(wallColor, roomColors, formatName))
        }"
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

        val DEFAULT_ROOM_PALETTE = listOf(RR_BLUE, RR_ORANGE, RR_GREEN, RR_YELLOW)
    }
}