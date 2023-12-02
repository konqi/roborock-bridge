package de.konqi.roborockbridge.roborockbridge.persistence

import de.konqi.roborockbridge.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Robot
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Schema
import org.springframework.data.repository.CrudRepository
import java.util.*

interface HomeRepository : CrudRepository<Home, Int>
interface RoomRepository : CrudRepository<Room, Int>
interface RobotRepository: CrudRepository<Robot, Int> {
    fun getByDeviceId(deviceId: String): Optional<Robot>
}
interface SchemaRepository : CrudRepository<Schema, Int>