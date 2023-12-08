package de.konqi.roborockbridge.roborockbridge.persistence

import de.konqi.roborockbridge.roborockbridge.persistence.entity.*
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.login.HomeDetailData
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.user.UserHomeData
import de.konqi.roborockbridge.roborockbridge.protocol.rest.dto.user.UserSchema
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class DataAccessLayer(
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository,
    private val deviceRepository: DeviceRepository,
    private val schemaRepository: SchemaRepository,
    private val deviceStateRepository: DeviceStateRepository,
) {
    fun saveSchemas(
        schemasFromRoborock: List<UserSchema>,
        homeEntity: Home
    ): MutableIterable<Schema> {
        return schemaRepository.saveAll(schemasFromRoborock.map { schema ->
            Schema(
                home = homeEntity,
                schemaId = schema.id,
                name = schema.name
            )
        })

    }

    fun saveRooms(
        homeDetails: UserHomeData,
        homeEntity: Home
    ): MutableIterable<Room> {
        return roomRepository.saveAll(
            homeDetails.rooms.map { Room(home = homeEntity, roomId = it.id, name = it.name) }
        )
    }

    fun saveHome(home: HomeDetailData): Home {
        return homeRepository.save(Home(homeId = home.rrHomeId, name = home.name))
    }

    @Transactional
    fun saveDevices(homeDetails: UserHomeData, home: Home): List<Device> {
        return homeDetails.devices.map { device ->
            val product = homeDetails.products.find { product -> product.id == device.productId }
                ?: throw RuntimeException("Unable to resolve product information for product id '${device.productId}'")

            val newDevice = deviceRepository.save(
                Device(
                    home = home,
                    deviceId = device.duid,
                    name = device.name,
                    deviceKey = device.localKey,
                    productName = product.name,
                    model = product.model,
                    firmwareVersion = device.fv,
                    serialNumber = device.sn,
                )
            )

            val states = deviceStateRepository.saveAll(
                device.deviceStatus.map { newState ->
                    val protocolInfo = product.schema.find { it.id.toInt() == newState.key }
                        ?: throw RuntimeException("Unable to resolve state meta data")

                    DeviceState(
                        device = newDevice,
                        schemaId = newState.key,
                        code = protocolInfo.code,
                        value = newState.value,
                        mode = ProtocolMode.valueOf(protocolInfo.mode.uppercase()),
                        property = protocolInfo.property,
                        type = protocolInfo.type
                    )
                }
            )

            newDevice.copy(state = states.toList())
        }
    }

    fun getDevice(deviceId: String) = deviceRepository.findById(deviceId)

    @Transactional
    fun updateDeviceState(deviceId: String, schemaId: Int, newValue: Int) =
        deviceStateRepository.updateStatus(deviceId, schemaId, newValue)
}