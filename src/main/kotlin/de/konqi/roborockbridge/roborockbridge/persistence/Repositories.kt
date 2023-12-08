package de.konqi.roborockbridge.roborockbridge.persistence

import de.konqi.roborockbridge.roborockbridge.persistence.entity.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface HomeRepository : CrudRepository<Home, Int>
interface RoomRepository : CrudRepository<Room, Int>
interface DeviceRepository: CrudRepository<Device, String>
interface DeviceStateRepository: CrudRepository<DeviceState, DeviceStateId> {
    @Modifying
    @Query("update DeviceState state set state.value = :current_value where state.device = :device_id and state.schemaId = :schema_id")
    fun updateStatus(@Param(value = "device_id") deviceId: String, @Param(value = "schema_id") schemaId: Int, @Param(value = "current_value") currentValue: Int): Int
}
interface SchemaRepository : CrudRepository<Schema, Int>