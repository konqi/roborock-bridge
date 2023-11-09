package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

data class UserHomeData(
    val id: Long,
    val name: String,
    val lon: Any?,
    val lat: Any?,
    val geoName: Any?,
    val products: List<Product>,
    val devices: List<Device>,
    val receivedDevices: List<Any?>,
    val rooms: List<Room>,
)
