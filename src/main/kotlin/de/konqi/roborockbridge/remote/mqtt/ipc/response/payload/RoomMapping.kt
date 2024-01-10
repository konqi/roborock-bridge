package de.konqi.roborockbridge.remote.mqtt.ipc.response.payload

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonFormat(shape= JsonFormat.Shape.ARRAY)
@JsonPropertyOrder(value = [ "mqttRoomId", "restRoomId", "unknownId" ])
data class RoomMapping(
    val mqttRoomId: Int,
    val restRoomId: String,
    val unknownId: Int
)
