package de.konqi.roborockbridge.persistence

import de.konqi.roborockbridge.persistence.entity.*
import de.konqi.roborockbridge.protocol.rest.dto.login.HomeDetailData
import de.konqi.roborockbridge.protocol.rest.dto.user.UserHomeData
import de.konqi.roborockbridge.protocol.rest.dto.user.UserSchema
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
                product.schema.filter {
                    // ignore ipc request and response
                    it.id.toInt() > 102
                } .map {
                    DeviceState(
                        device = newDevice,
                        schemaId = it.id.toInt(),
                        code = it.code,
                        mode = ProtocolMode.valueOf(it.mode.uppercase()),
                        type = it.type,
                        property = it.property,
                        value = device.deviceStatus[it.id.toInt()] ?: -1
                    )
                }
            )

            newDevice.copy(state = states.toList())
        }
    }

    fun getDevice(deviceId: String) = deviceRepository.findById(deviceId)

    @Transactional
    fun updateDeviceState(deviceId: String, schemaId: Int, newValue: Int) =
        deviceStateRepository.updateStatus(DeviceStateId(deviceId, schemaId), newValue)
}