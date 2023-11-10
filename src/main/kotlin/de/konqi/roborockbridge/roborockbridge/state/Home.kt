package de.konqi.roborockbridge.roborockbridge.state

import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Room
import kotlinx.serialization.Serializable

@Serializable
data class Home(
    val rooms: List<Room>
)
