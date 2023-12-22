package de.konqi.roborockbridge.persistence

import de.konqi.roborockbridge.persistence.entity.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.*

interface HomeRepository : CrudRepository<Home, Int>
interface RoomRepository : CrudRepository<Room, Int>
interface DeviceRepository : CrudRepository<Device, String>
interface DeviceStateRepository : CrudRepository<DeviceState, DeviceStateId> {
    /**
     * Update only if actual change
     */
    @Modifying
    @Query("update DeviceState state set state.value = :current_value where state.device.deviceId = :#{#id.device} and state.code = :#{#id.code} and state.value != :current_value")
    fun updateStatus(@Param(value = "id") id: DeviceStateId, @Param(value = "current_value") currentValue: Int): Int

    fun findAllByDevice_DeviceIdAndModifiedDateAfter(deviceId: String, afterDate: Date): List<DeviceState>
    fun findAllByDevice_DeviceId(deviceId: String): List<DeviceState>

}

interface SchemaRepository : CrudRepository<Schema, Int>