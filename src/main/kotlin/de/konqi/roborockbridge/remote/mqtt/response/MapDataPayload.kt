package de.konqi.roborockbridge.remote.mqtt.response

import de.konqi.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.remote.mqtt.response.map.*

data class MapDataPayload(
    val map: MapDataImage? = null,
    val robotPosition: MapDataObjectPosition<Float>? = null,
    val chargerPosition: MapDataObjectPosition<Float>? = null,
    val path: MapDataPath<Float>? = null,
    val gotoPath: MapDataPath<Float>? = null,
    val predictedPath: MapDataPath<Float>? = null,
    val currentlyCleanedZones: MapDataRectangles<Float>? = null,
    val gotoTarget: MapDataGotoTarget<Float>? = null,
    val virtualWalls: MapDataRectangles<Float>? = null,
    val noGoAreas: MapDataArea<Float>? = null,
    val noMoppingArea: MapDataArea<Float>? = null,
    val obstacles: List<Obstacle<Float>> = emptyList()
) {
    companion object {
        private val logger by LoggerDelegate()

        fun fromRawBytes(data: ByteArray): MapDataPayload {
            var map: MapDataImage? = null
            var chargerPosition: MapDataObjectPosition<UInt>? = null
            var robotPosition: MapDataObjectPosition<UInt>? = null
            var gotoTarget: MapDataGotoTarget<UShort>? = null
            var virtualWalls: MapDataRectangles<UShort>? = null
            var path: MapDataPath<UShort>? = null
            var gotoPath: MapDataPath<UShort>? = null
            var predictedPath: MapDataPath<UShort>? = null
            var currentlyCleanedZones: MapDataRectangles<UShort>? = null
            var noGoAreas: MapDataArea<UShort>? = null
            var noMoppingArea: MapDataArea<UShort>? = null
            val obstacles: List<Obstacle<UShort>> = mutableListOf()


            val mapData = MapData(data)
            while (mapData.body.remaining() > 0) {
                val section = MapDataSection.fromRaw(mapData.body)

                when (section.type) {
                    SectionType.IMAGE -> map = MapDataImage(section)
                    SectionType.CHARGER -> chargerPosition = MapDataObjectPosition.fromRawMapDataSection(section)
                    SectionType.ROBOT_POSITION -> robotPosition = MapDataObjectPosition.fromRawMapDataSection(section)
                    SectionType.PATH -> path = MapDataPath.fromRawMapDataSection(section)
                    SectionType.GOTO_PATH -> gotoPath = MapDataPath.fromRawMapDataSection(section)
                    SectionType.GOTO_PREDICTED_PATH -> predictedPath = MapDataPath.fromRawMapDataSection(section)
                    SectionType.CURRENTLY_CLEANED_ZONES -> currentlyCleanedZones =
                        MapDataRectangles.fromMapDataSection(section)

                    SectionType.GOTO_TARGET -> gotoTarget = MapDataGotoTarget.fromMapDataSection(section)
                    SectionType.VIRTUAL_WALLS -> virtualWalls = MapDataRectangles.fromMapDataSection(section)
                    SectionType.NO_GO_AREAS -> noGoAreas = MapDataArea.fromMapDataSection(section)
                    SectionType.NO_MOPPING_AREAS -> noMoppingArea = MapDataArea.fromMapDataSection(section)
                    SectionType.CARPET_MAP -> { /* bitmap of carpet */
                        // guess i'll need a carpet first
                    }

                    SectionType.OBSTACLES,
                    SectionType.IGNORED_OBSTACLES,
                    SectionType.OBSTACLES_WITH_PHOTO,
                    SectionType.IGNORED_OBSTACLES_WITH_PHOTO -> {
                        // TODO
                        val obstacle = MapDataObstacle.fromRawMapDataSection(section)
                        if (obstacle.numberOfObstacles > 0u) {
                            obstacle.obstacles.forEach(obstacles::addLast)
                        }
                    }

                    SectionType.UNKNOWN28,
                    SectionType.UNKNOWN29,
                    SectionType.UNKNOWN30,
                    SectionType.UNKNOWN31,
                    SectionType.UNKNOWN32,
                    SectionType.UNKNOWN33,
                    SectionType.MOP_PATH,
                    SectionType.SMART_ZONE,
                    SectionType.CARPET_FORBIDDEN,
                    SectionType.FLOOR_MAP,
                    SectionType.FURNITURE,
                    SectionType.DOCK_TYPE,
                    SectionType.CUSTOM_CARPET -> {
                        // ignored sections
                        // TODO unignore some?
                        logger.debug("Ignored section type in 301 response: ${section.typeNumber} (${section.type})")
//                        val header = ByteArray(section.headerLength.toInt()).also { section.header.get(it) }
//                        val data = ByteArray(section.bodyLength.toInt()).also { section.body.get(it) }
                    }
                    // TODO Blocks?

                    SectionType.DIGEST -> {
                        // actually handled already
                    }

                    else -> {
                        logger.warn("Unknown/unhandled section type in 301 response: ${section.typeNumber} (${section.type})")
                    }
                }

                mapData.body.position(mapData.body.position() + section.headerLength.toInt() + section.bodyLength.toInt())
            }

            val top: UInt = map?.top ?: 0u
            val left: UInt = map?.left ?: 0u

            return MapDataPayload(
                map = map,
                robotPosition = robotPosition?.correct(top, left),
                chargerPosition = chargerPosition?.correct(top, left),
                path = path?.correct(top.toUShort(), left.toUShort()),
                gotoPath = gotoPath?.correct(top.toUShort(), left.toUShort()),
                predictedPath = predictedPath?.correct(top.toUShort(), left.toUShort()),
                currentlyCleanedZones = currentlyCleanedZones?.correct(top.toUShort(), left.toUShort()),
                gotoTarget = gotoTarget?.correct(top.toUShort(), left.toUShort()),
                virtualWalls = virtualWalls?.correct(top.toUShort(), left.toUShort()),
                noGoAreas = noGoAreas?.correct(top.toUShort(), left.toUShort()),
                noMoppingArea = noMoppingArea?.correct(top.toUShort(), left.toUShort()),
                obstacles = obstacles.map { it.correct(top.toUShort(), left.toUShort()) },
            )
        }
    }
}