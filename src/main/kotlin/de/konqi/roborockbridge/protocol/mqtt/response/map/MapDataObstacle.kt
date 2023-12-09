package de.konqi.roborockbridge.protocol.mqtt.response.map

data class Obstacle(
    val x: UShort,
    val y: UShort,
    val type: ObstacleType? = null,
    val u1: Short? = null,
    val u2: Short? = null,
    val photo: String? = null
)

class MapDataObstacle(data: MapDataSection) {
    val numberOfObstacles = data.header.getShort(8).toUShort()
    val obstacles = if (numberOfObstacles > 0u) (data.bodyLength / numberOfObstacles).let { sizeOfObstacleInBytes ->
        List(numberOfObstacles.toInt()) { index ->
            val x = data.body.getShort(index * sizeOfObstacleInBytes.toInt()).toUShort()
            val y = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 2).toUShort()

            val obstacle = mutableMapOf<String, Any?>(
                "x" to x,
                "y" to y
            )

            val type = if (sizeOfObstacleInBytes >= 6u) {
                ObstacleType.fromValue(data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 4).toUShort())
            } else null

            val (u1, u2) = if (sizeOfObstacleInBytes >= 10u) {
                val u1 = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 6)
                val u2 = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 8)
                u1 to u2
            } else null to null

            // TODO: Investigate the Short?-value at offset 10. Could be string length
            val photo =
                if (sizeOfObstacleInBytes == 28u && data.body.get(index * sizeOfObstacleInBytes.toInt() + 12) > 0) {
                    charset("ascii").decode(data.body.duplicate().position(12).slice().limit(16)).toString()
                } else null

            Obstacle(x = x, y = y, type = type, u1 = u1, u2 = u2, photo = photo)
        }
    } else emptyList()
}