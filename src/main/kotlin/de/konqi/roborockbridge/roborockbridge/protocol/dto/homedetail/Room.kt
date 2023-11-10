package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: Long,
    val name: String,
)