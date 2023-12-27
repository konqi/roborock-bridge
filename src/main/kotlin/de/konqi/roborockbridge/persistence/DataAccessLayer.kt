package de.konqi.roborockbridge.persistence

import de.konqi.roborockbridge.persistence.entity.*
import de.konqi.roborockbridge.remote.rest.dto.login.HomeDetailData
import de.konqi.roborockbridge.remote.rest.dto.user.UserHomeData
import de.konqi.roborockbridge.remote.rest.dto.user.UserScenes
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.*

@Component
class DataAccessLayer(
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository,
    private val deviceRepository: DeviceRepository,
    private val schemaRepository: SchemaRepository,
    private val deviceStateRepository: DeviceStateRepository,
) {
    fun saveRoutines(
        schemasFromRoborock: List<UserScenes>, homeEntity: Home
    ): MutableIterable<Routine> {
        return schemaRepository.saveAll(schemasFromRoborock.map { schema ->
            Routine(
                home = homeEntity, routineId = schema.id, name = schema.name
            )
        })
    }

    fun saveRooms(
        homeDetails: UserHomeData, homeEntity: Home
    ): MutableIterable<Room> {
        return roomRepository.saveAll(homeDetails.rooms.map { Room(home = homeEntity, roomId = it.id, name = it.name) })
    }

    fun getHomes(): Iterable<Home> = homeRepository.findAll()

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

            val states = deviceStateRepository.saveAll(product.schema.filter {
                // ignore ipc request and response
                it.id.toInt() > 102
            }.map {
                DeviceState(
                    device = newDevice,
                    schemaId = it.id.toInt(),
                    code = it.code,
                    mode = ProtocolMode.valueOf(it.mode.uppercase()),
                    type = it.type,
                    property = it.property,
                    value = device.deviceStatus[it.id.toInt()] ?: -1
                )
            })

            newDevice.copy(state = states.toList())
        }
    }

    fun getDevice(deviceId: String) = deviceRepository.findById(deviceId)

    fun getAllDeviceStatesModifiedAfterDate(deviceId: String, modifiedAfter: Date? = null) =
        if (modifiedAfter != null) deviceStateRepository.findAllByDevice_DeviceIdAndModifiedDateAfter(
            deviceId, modifiedAfter
        )
        else deviceStateRepository.findAllByDevice_DeviceId(deviceId)

    @Transactional
    fun updateDeviceState(deviceId: String, code: String, newValue: Int) =
        deviceStateRepository.updateStatus(DeviceStateId(deviceId, code), newValue)

    /**
     * Performs only required operations, thus leaving modified_date intact
     */
    @Transactional
    fun updateDeviceStates(deviceId: String, states: Map<String, Int>) {
        val codes = states.keys

        deviceRepository.findById(deviceId).ifPresent { device ->
            val existingDeviceStateEntries = device.state.filter { it.code in codes }
            val updateCandidates = existingDeviceStateEntries.filter { it.value != states[it.code] }.map {
                it.copy(
                    // guaranteed to exist in states, since that is the base of the first filter
                    value = states[it.code]!!
                )
            }

            val existingCodes = existingDeviceStateEntries.map { it.code }
            val creationCandidates = states.filter { it.key !in existingCodes }.map { (code, newValue) ->
                DeviceState(device = device, code = code, value = newValue)
            }

            val entitiesToSave = updateCandidates + creationCandidates
            deviceStateRepository.saveAll(entitiesToSave)
        }
    }
}