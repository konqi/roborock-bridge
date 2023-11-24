package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import org.springframework.security.crypto.codec.Hex
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

enum class SectionType(val id: UShort) {
    CHARGER(1u),
    IMAGE(2u),
    PATH(3u),
    GOTO_PATH(4u),
    GOTO_PREDICTED_PATH(5u),
    CURRENTLY_CLEANED_ZONES(6u),
    GOTO_TARGET(7u),
    ROBOT_POSITION(8u),
    NO_GO_AREAS(9u),
    VIRTUAL_WALLS(10u),
    BLOCKS(11u),
    NO_MOPPING_AREAS(12u),
    OBSTACLES(13u),
    IGNORED_OBSTACLES(14u),
    OBSTACLES_WITH_PHOTO(15u),
    IGNORED_OBSTACLES_WITH_PHOTO(16u),
    CARPET_MAP(17u),
    MOP_PATH(18u),
    CARPET_FORBIDDEN(19u),
    SMART_ZONE_PATH_TYPE(20u),
    SMART_ZONE(21u),
    CUSTOM_CARPET(22u),
    CL_FORBIDDEN_ZONES(23u),
    FLOOR_MAP(24u),
    FURNITURE(25u),
    DOCK_TYPE(26u),
    ENEMIES(27u),
    UNKNOWN28(28u),
    UNKNOWN29(29u),
    UNKNOWN30(30u),
    UNKNOWN31(31u),
    UNKNOWN32(32u),
    UNKNOWN33(33u),
    DIGEST(1024u);

    companion object {
        private val mapping = entries.associateBy(SectionType::id)
        fun fromValue(value: UShort) = mapping[value]
    }
}

class MapData(val data: ByteArray) {
    val buffer = ByteBuffer.wrap(data).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN).mark()

    val preamble = String(ByteArray(2).apply { buffer.get(this) })

    val headerLength = buffer.getShort().toUShort()
    val bodyLength = buffer.getInt().toUInt()

    // wrap byte buffer around existing data to save memory
    val header = buffer.reset().slice().limit(headerLength.toInt()).order(ByteOrder.LITTLE_ENDIAN)
    val body = buffer.duplicate().position(headerLength.toInt()).slice().order(ByteOrder.LITTLE_ENDIAN)
    val digest = String(Hex.encode(data.copyOfRange(data.size - 20, data.size)))
    val calculatedDigest: String
        get() = String(
            Hex.encode(
                MessageDigest.getInstance("SHA-1").digest(data.copyOf(buffer.limit() - 20))
            )
        )


    val version = "${header.getShort(8).toUShort()}.${header.getShort(10).toUShort()}"
    val mapIndex = header.getInt(12).toUInt()
    val mapSequence = header.getInt(16).toUInt()

    init {
        if (preamble != "rr") {
            throw RuntimeException("Packet data does not seem to be map data. Incorrect preamble")
        }
        if (digest != calculatedDigest) {
            throw RuntimeException("Packet data corrupt. Mismatching digest")
        }
    }
}

class MapDataSection(buffer: ByteBuffer) {
    val slice: ByteBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
    val type = slice.getShort(0).toUShort()
    val headerLength = slice.getShort(2).toUShort()
    val bodyLength = slice.getInt(4).toUInt()

    val header = slice.duplicate().limit(headerLength.toInt()).order(ByteOrder.LITTLE_ENDIAN)
    val body = slice.duplicate().position(headerLength.toInt()).slice().order(ByteOrder.LITTLE_ENDIAN)
        .limit(bodyLength.toInt())

    fun getResolvedType(): SectionType? {
        return SectionType.fromValue(type)
    }
}

data class Room(
    var xMin: Int,
    var xMax: Int,
    var yMin: Int,
    var yMax: Int
)

class MapDataObjectPosition(data: MapDataSection) {
    val x = data.body.getInt(0).toUInt()
    val y = data.body.getInt(UInt.SIZE_BYTES).toUInt()
    val a = if (data.bodyLength > UInt.SIZE_BYTES.toUInt() * 2u) data.body.getInt(2 * UInt.SIZE_BYTES) else null
}

class MapDataPath(data: MapDataSection) {
    val end = data.header.getInt(4).toUInt()
    val length = data.header.getInt(8).toUInt()
    val size = data.header.getInt(12).toUInt()
    val angle = data.header.getInt(16).toUInt()
    val points = List<Pair<UShort, UShort>>(length.toInt()) { index ->
        data.body.getShort(index * 4).toUShort() to data.body.getShort(index * 4 + 2).toUShort()
    }
}

class MapDataObstacle(data: MapDataSection) {
    // TODO
}

class MapDataArea(data: MapDataSection) {
    // TODO
}

class MapDataWalls(data: MapDataSection) {
    // TODO
}

class MapDataZones(data: MapDataSection) {
    // TODO
}

class MapDataGotoTarget(data: MapDataSection) {
    val x = data.body.getShort(0).toUShort()
    val y = data.body.getShort(UShort.SIZE_BYTES).toUShort()
}

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

class Response301(data: ByteArray) {
    val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    val endpoint: String = String(ByteArray(15).also { buffer.get(it) }).trimEnd()
    val unknownNumber: UByte = buffer.get().toUByte()
    val id: UShort = buffer.getShort().toUShort()
    val unknownBytes = ByteArray(6).also { buffer.get(it) }
    val payload = ByteArray(buffer.remaining()).also { buffer.get(it) }

    fun decrypt(key: ByteArray): ByteArray {
        val iv = IvParameterSpec(ByteArray(16) { 0 })
        val cipher = Cipher.getInstance(CIPHER).also {
            it.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"), iv
            )
        }

        val decrypted = CipherInputStream(payload.inputStream(), cipher).use {
            GZIPInputStream(it).readBytes()
        }

        val mapData = MapData(decrypted)
        while (mapData.body.remaining() > 0) {
            val section = MapDataSection(mapData.body)
            println("${section.type}=${section.getResolvedType()} ${section.body.limit()}")

            when (section.getResolvedType()) {
                SectionType.IMAGE -> {
                    val image = MapDataImage(section)
                    println(image)
                }

                SectionType.CHARGER -> {
                    val pos = MapDataObjectPosition(section)
                    println(pos)
                }

                SectionType.ROBOT_POSITION -> {
                    val pos = MapDataObjectPosition(section)
                    println(pos)
                }

                SectionType.PATH -> {
                    val path = MapDataPath(section)
                    println(path)
                }

                SectionType.GOTO_PATH -> {
                    val path = MapDataPath(section)
                    println(path)
                }

                SectionType.GOTO_PREDICTED_PATH -> {
                    val path = MapDataPath(section)
                    println(path)
                }

                SectionType.CURRENTLY_CLEANED_ZONES -> {
                    val zone = MapDataZones(section)
                    println(zone)
                }

                SectionType.GOTO_TARGET -> {
                    val target = MapDataGotoTarget(section)
                    println(target)
                }

                SectionType.VIRTUAL_WALLS -> {
                    MapDataWalls(section)
                }

                SectionType.NO_GO_AREAS -> {
                    MapDataArea(section)
                }

                SectionType.NO_MOPPING_AREAS -> {
                    MapDataArea(section)
                }

                in arrayOf(SectionType.OBSTACLES, SectionType.IGNORED_OBSTACLES, SectionType.OBSTACLES_WITH_PHOTO, SectionType.IGNORED_OBSTACLES_WITH_PHOTO) -> {
                    val obstacle = MapDataObstacle(section)
                    println(obstacle)
                }

//                SectionType.OBSTACLES -> {
//                    MapDataObstacle(section)
//                }
//
//                SectionType.IGNORED_OBSTACLES -> {
//                    MapDataObstacle(section)
//                }
//
//                SectionType.OBSTACLES_WITH_PHOTO -> {
//                    MapDataObstacle(section)
//                }
//
//                SectionType.IGNORED_OBSTACLES_WITH_PHOTO -> {
//                    MapDataObstacle(section)
//                }

                // TODO Blocks?

                else -> {
                    println(section.getResolvedType())
                }
            }

            mapData.body.position(mapData.body.position() + section.headerLength.toInt() + section.bodyLength.toInt())
        }

        return decrypted
    }

    companion object {
        const val CIPHER = "AES/CBC/PKCS5Padding"
    }
}