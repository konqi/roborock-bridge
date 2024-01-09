package de.konqi.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.io.Serializable

data class RoutineId(
    val home: Int? = null,
    val routineId: Int? = null
) : Serializable

@Entity
@IdClass(RoutineId::class)
data class Routine(
    @Id
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Id
    @Column(name = "routine_id", nullable = false)
    val routineId: Int,
    val name: String,

    @ElementCollection
    @CollectionTable(
        name="routine_device",
        joinColumns=[JoinColumn(name = "routine_id"), JoinColumn(name = "home_id")]
    )
    val triggeredDeviceIds: Set<String> = setOf()
)