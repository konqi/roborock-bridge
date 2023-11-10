package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val model: String,
    val iconUrl: String?,
    val attribute: String?,
    val capability: Long,
    val category: String,
    val schema: List<Schema>,
)