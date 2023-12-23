package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.persistence.entity.Device

data class DeviceForPublish(
    val homeId: Int,
    val deviceId: String,
    val deviceKey: String,
    val name: String,
    val productName: String,
    val model: String,
    val firmwareVersion: String,
    val serialNumber: String,
) {
    companion object {
        fun fromDeviceEntity(device: Device) = DeviceForPublish(
            homeId = device.home.homeId,
            deviceId = device.deviceId,
            deviceKey = device.deviceKey,
            name = device.name,
            productName = device.productName,
            model = device.model,
            firmwareVersion = device.firmwareVersion,
            serialNumber = device.serialNumber,
        )
    }
}