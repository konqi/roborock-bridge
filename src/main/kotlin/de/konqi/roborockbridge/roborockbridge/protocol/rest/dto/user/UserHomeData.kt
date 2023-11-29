package de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.user

import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Device
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Product
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.homedetail.Room

data class UserHomeData(
    val id: Long,
    val name: String,
    // probably a decimal value
    val lon: String?,
    // probably a decimal value
    val lat: String?,
    // unknown (maybe a city name? planet? solar system?)
    val geoName: String?,
    val products: List<Product>,
    val devices: List<Device>,
    // possibly wrong
    val receivedDevices: List<String?>,
    val rooms: List<Room>,
)