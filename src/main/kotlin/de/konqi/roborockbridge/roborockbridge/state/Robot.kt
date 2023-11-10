package de.konqi.roborockbridge.roborockbridge.state

import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Device
import de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail.Product
import kotlinx.serialization.Serializable

@Serializable
data class Robot (
    val deviceId: String,
    val deviceName: String,
    val deviceInformation: Device,
    val productInformation: Product
)