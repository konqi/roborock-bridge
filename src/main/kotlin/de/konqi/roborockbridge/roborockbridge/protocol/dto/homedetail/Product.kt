package de.konqi.roborockbridge.roborockbridge.protocol.dto.homedetail

data class Product(
    val id: String,
    val name: String,
    val model: String,
    val iconUrl: Any?,
    val attribute: Any?,
    val capability: Long,
    val category: String,
    val schema: List<Schema>,
)