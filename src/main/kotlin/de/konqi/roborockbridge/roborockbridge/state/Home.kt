package de.konqi.roborockbridge.roborockbridge.state

import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Room

data class Home(
    val rooms: List<Room>
)
