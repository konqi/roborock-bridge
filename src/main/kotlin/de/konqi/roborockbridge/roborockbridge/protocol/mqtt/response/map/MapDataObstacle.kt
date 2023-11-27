package de.konqi.roborockbridge.roborockbridge.protocol.mqtt.response.map

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

            if (sizeOfObstacleInBytes >= 6u) {
                val type = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 4).toUShort()
                obstacle["type"] = ObstacleType.fromValue(type)
            }

            if (sizeOfObstacleInBytes >= 10u) {
                val u1 = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 6)
                val u2 = data.body.getShort(index * sizeOfObstacleInBytes.toInt() + 8)
                obstacle["u1"] = u1
                obstacle["u2"] = u2
            }
            // TODO: Investigate the Short?-value at offset 10. Could be string length
            if (sizeOfObstacleInBytes == 28u && data.body.get(index * sizeOfObstacleInBytes.toInt() + 12) > 0) {
                val txt = charset("ascii").decode(data.body.duplicate().position(12).slice().limit(16))
                obstacle["photo"] = txt
            }

            obstacle
        }
    } else emptyList()
}