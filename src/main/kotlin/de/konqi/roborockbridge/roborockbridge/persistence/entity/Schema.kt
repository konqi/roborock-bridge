package de.konqi.roborockbridge.roborockbridge.persistence.entity

import jakarta.persistence.*

@Entity
class Schema(
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Column(name = "schema_id", nullable = false)
    val schemaId: Int,
    val name: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)