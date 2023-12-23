package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.persistence.entity.Schema

data class SchemaForPublish(val homeId: Int, val id: Int, val name: String) {
    companion object {
        fun fromSchemaEntity(schema: Schema) = SchemaForPublish(
                homeId = schema.home.homeId,
                id = schema.schemaId,
                name = schema.name
            )
    }
}