package de.konqi.roborockbridge.protocol.mqtt.response.map

enum class ObstacleType(val id: UShort) {
    CABLE(0u),
    SHOES(2u),
    POOP(3u),
    EXTENSION_CORD(5u),
    WEIGHING_SCALE(9u),
    CLOTHING(10u);

    companion object {
        private val mapping = ObstacleType.entries.associateBy(ObstacleType::id)
        fun fromValue(value: UShort) = mapping[value]
    }
}