package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response

import de.konqi.roborockbridge.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map.*

data class Protocol301Payload(
    var map: MapDataImage? = null,
    var robotPosition: MapDataObjectPosition? = null,
    var chargerPosition: MapDataObjectPosition? = null,
    var mapDataPath: MapDataPath? = null,
    var mapDataZones: MapDataZones? = null,
    var gotoTarget: MapDataGotoTarget? = null,
    var virtualWalls: MapDataWalls? = null,
    var noGoAreas: MapDataArea? = null,
    var noMoppingArea: MapDataArea? = null,
    var mapDataObstacle: MapDataObstacle? = null
) {
    companion object {
        private val logger by LoggerDelegate()

        fun fromRawBytes(data: ByteArray): Protocol301Payload {
            val decodedResponseData = Protocol301Payload()

            val mapData = MapData(data)
            while (mapData.body.remaining() > 0) {
                val section = MapDataSection(mapData.body)

                when (section.type) {
                    SectionType.IMAGE -> {
                        decodedResponseData.map = MapDataImage(section)
                    }

                    SectionType.CHARGER -> {
                        decodedResponseData.chargerPosition = MapDataObjectPosition(section)
                    }

                    SectionType.ROBOT_POSITION -> {
                        decodedResponseData.robotPosition = MapDataObjectPosition(section)
                    }

                    in arrayOf(SectionType.PATH, SectionType.GOTO_PATH, SectionType.GOTO_PREDICTED_PATH) -> {
                        // TODO
                        val path = MapDataPath(section)
                    }

                    SectionType.CURRENTLY_CLEANED_ZONES -> {
                        // TODO
                        val zone = MapDataZones(section)
                    }

                    SectionType.GOTO_TARGET -> {
                        decodedResponseData.gotoTarget = MapDataGotoTarget(section)
                    }

                    SectionType.VIRTUAL_WALLS -> {
                        decodedResponseData.virtualWalls = MapDataWalls(section)
                    }

                    in arrayOf(SectionType.NO_GO_AREAS, SectionType.NO_MOPPING_AREAS) -> {
                        // TODO
                        MapDataArea(section)
                    }

                    in arrayOf(
                        SectionType.OBSTACLES,
                        SectionType.IGNORED_OBSTACLES,
                        SectionType.OBSTACLES_WITH_PHOTO,
                        SectionType.IGNORED_OBSTACLES_WITH_PHOTO
                    ) -> {
                        // TODO
                        val obstacle = MapDataObstacle(section)
                    }

                    in arrayOf(
                        SectionType.DIGEST,
                        SectionType.UNKNOWN28,
                        SectionType.UNKNOWN29,
                        SectionType.UNKNOWN30,
                        SectionType.UNKNOWN31,
                        SectionType.UNKNOWN32,
                        SectionType.UNKNOWN33,
                        SectionType.MOP_PATH,
                        SectionType.SMART_ZONE,
                        SectionType.CARPET_MAP,
                        SectionType.CARPET_FORBIDDEN,
                        SectionType.FLOOR_MAP,
                        SectionType.FURNITURE,
                        SectionType.DOCK_TYPE,
                        SectionType.CUSTOM_CARPET
                    ) -> {
                        // ignored sections
                        // TODO unignore some?
                    }
                    // TODO Blocks?

                    else -> {
                        logger.warn("Unknown/unhandled section type in 301 response: ${section.typeNumber} (${section.type})")
                    }
                }

                mapData.body.position(mapData.body.position() + section.headerLength.toInt() + section.bodyLength.toInt())
            }

            return decodedResponseData

        }
    }
}