package de.konqi.roborockbridge.bridge.dto

import com.fasterxml.jackson.annotation.JsonInclude
import de.konqi.roborockbridge.persistence.entity.Room

@JsonInclude(value =  JsonInclude.Include.NON_NULL)
data class RoomForPublish(val homeId: Int, val roomId: Int, val name: String, val mqttRoomId: Int? = null) {
    companion object {
        fun fromRoomEntity(room: Room) = RoomForPublish(
            homeId = room.home.homeId,
            roomId = room.roomId,
            name = room.name,
            mqttRoomId = room.mqttRoomId
        )
    }
}