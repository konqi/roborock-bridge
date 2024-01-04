package de.konqi.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.io.Serializable

data class RoomId(
    val home: Int? = null,
    val roomId: Int? = null
) : Serializable

@Entity
@IdClass(RoomId::class)
data class Room(
    @Id
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Id
    @Column(name = "room_id", nullable = false)
    val roomId: Int,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = true)
    val mqttRoomId: Int? = null
)