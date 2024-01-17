package de.konqi.roborockbridge.bridge.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import de.konqi.roborockbridge.persistence.entity.Room

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class RoomForPublish(
    @get:JsonIgnore
    val homeId: Int,
    @get:JsonProperty(value = "room_id")
    val roomId: Int,
    val name: String,
    @get:JsonProperty(value = "mqtt_room_id")
    val mqttRoomId: Int? = null
) {
    companion object {
        fun fromRoomEntity(room: Room) = RoomForPublish(
            homeId = room.home.homeId,
            roomId = room.roomId,
            name = room.name,
            mqttRoomId = room.mqttRoomId
        )
    }
}