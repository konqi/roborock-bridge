package de.konqi.roborockbridge.protocol.mqtt.response

import de.konqi.roborockbridge.LoggerDelegate
import de.konqi.roborockbridge.protocol.mqtt.response.map.*

data class Protocol301Payload(
    val map: MapDataImage? = null,
    val robotPosition: MapDataObjectPosition? = null,
    val chargerPosition: MapDataObjectPosition? = null,
    val path: MapDataPath? = null,
    val gotoPath: MapDataPath? = null,
    val predictedPath: MapDataPath? = null,
    val currentlyCleanedZones: MapDataZones? = null,
    val gotoTarget: MapDataGotoTarget? = null,
    val virtualWalls: MapDataWalls? = null,
    val noGoAreas: MapDataArea? = null,
    val noMoppingArea: MapDataArea? = null,
    val obstacles: List<Obstacle> = emptyList()
) {
    companion object {
        private val logger by LoggerDelegate()

        fun fromRawBytes(data: ByteArray): Protocol301Payload {
            var map: MapDataImage? = null
            var chargerPosition: MapDataObjectPosition? = null
            var robotPosition: MapDataObjectPosition? = null
            var gotoTarget: MapDataGotoTarget? = null
            var virtualWalls: MapDataWalls? = null
            var path: MapDataPath? = null
            var gotoPath: MapDataPath? = null
            var predictedPath: MapDataPath? = null
            var currentlyCleanedZones: MapDataZones? = null
            var noGoAreas: MapDataArea? = null
            var noMoppingArea: MapDataArea? = null
            val obstacles: List<Obstacle> = mutableListOf()


            val mapData = MapData(data)
            while (mapData.body.remaining() > 0) {
                val section = MapDataSection(mapData.body)

                when (section.type) {
                    SectionType.IMAGE -> {
                        map = MapDataImage(section)
                    }

                    SectionType.CHARGER -> {
                        chargerPosition = MapDataObjectPosition(section)
                    }

                    SectionType.ROBOT_POSITION -> {
                        robotPosition = MapDataObjectPosition(section)
                    }

                    in arrayOf(SectionType.PATH, SectionType.GOTO_PATH, SectionType.GOTO_PREDICTED_PATH) -> {
                        val parsedPath = MapDataPath(section)
                        if(section.type == SectionType.PATH) {
                            path = parsedPath
                        } else if(section.type == SectionType.GOTO_PATH) {
                            gotoPath = path
                        }
                        else if(section.type == SectionType.GOTO_PREDICTED_PATH) {
                            predictedPath = path
                        }
                    }

                    SectionType.CURRENTLY_CLEANED_ZONES -> {
                        currentlyCleanedZones = MapDataZones(section)
                    }

                    SectionType.GOTO_TARGET -> {
                        gotoTarget = MapDataGotoTarget(section)
                    }

                    SectionType.VIRTUAL_WALLS -> {
                        virtualWalls = MapDataWalls(section)
                    }

                    in arrayOf(SectionType.NO_GO_AREAS, SectionType.NO_MOPPING_AREAS) -> {
                        val area = MapDataArea(section)
                        if (section.type == SectionType.NO_GO_AREAS) {
                            noGoAreas = area
                        } else if (section.type == SectionType.NO_MOPPING_AREAS) {
                            noMoppingArea = area
                        }

                    }

                    in arrayOf(
                        SectionType.OBSTACLES,
                        SectionType.IGNORED_OBSTACLES,
                        SectionType.OBSTACLES_WITH_PHOTO,
                        SectionType.IGNORED_OBSTACLES_WITH_PHOTO
                    ) -> {
                        // TODO
                        val obstacle = MapDataObstacle(section)
                        if(obstacle.numberOfObstacles > 0u) {
                            obstacle.obstacles.forEach(obstacles::addLast)
                        }
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

            return Protocol301Payload(
                map = map,
                robotPosition = robotPosition,
                chargerPosition = chargerPosition,
                path = path,
                gotoPath = gotoPath,
                predictedPath = predictedPath,
                currentlyCleanedZones = currentlyCleanedZones,
                gotoTarget = gotoTarget,
                virtualWalls = virtualWalls,
                noGoAreas = noGoAreas,
                noMoppingArea = noMoppingArea,
                obstacles = obstacles,
            )

        }
    }
}