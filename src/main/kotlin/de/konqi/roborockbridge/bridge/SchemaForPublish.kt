package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.persistence.entity.Routine

data class SchemaForPublish(val homeId: Int, val id: Int, val name: String) {
    companion object {
        fun fromSchemaEntity(schema: Routine) = SchemaForPublish(
                homeId = schema.home.homeId,
                id = schema.routineId,
                name = schema.name
            )
    }
}