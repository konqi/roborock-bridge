package de.konqi.roborockbridge.protocol.mqtt.response.map

import java.nio.ByteBuffer
import java.nio.ByteOrder

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

data class MapDataSection(
    val typeNumber: UShort,
    val type: SectionType?,
    val headerLength: UShort,
    val bodyLength: UInt,
    val header: ByteBuffer,
    val body: ByteBuffer
) {

    companion object {
        fun fromRaw(buffer: ByteBuffer): MapDataSection {
            val slice: ByteBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)

            val typeNumber: UShort = slice.getShort(0).toUShort()
            val headerLength: UShort = slice.getShort(2).toUShort()
            val bodyLength: UInt = slice.getInt(4).toUInt()

            return MapDataSection(
                typeNumber = typeNumber,
                type = SectionType.fromValue(typeNumber),
                headerLength = headerLength,
                bodyLength = bodyLength,
                header = slice.duplicate().limit(headerLength.toInt()).order(ByteOrder.LITTLE_ENDIAN),
                body = slice.duplicate().position(headerLength.toInt()).slice().order(ByteOrder.LITTLE_ENDIAN)
                    .limit(bodyLength.toInt())
            )
        }
    }
}