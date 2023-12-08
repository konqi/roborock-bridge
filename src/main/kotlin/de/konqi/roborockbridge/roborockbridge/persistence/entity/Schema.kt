package de.konqi.roborockbridge.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["home_id", "schema_id"])])
data class Schema(
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Column(name = "schema_id", nullable = false)
    val schemaId: Int,
    val name: String,
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)