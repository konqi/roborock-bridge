package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import kotlinx.serialization.Serializable

@Serializable
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
