package de.konqi.roborockbridge.roborockbridge.persistence

import de.konqi.roborockbridge.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Robot
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Schema
import org.springframework.data.repository.CrudRepository

interface HomeRepository : CrudRepository<Home, Int>
interface RoomRepository : CrudRepository<Room, Int>
interface RobotRepository: CrudRepository<Robot, Int>
interface SchemaRepository : CrudRepository<Schema, Int>