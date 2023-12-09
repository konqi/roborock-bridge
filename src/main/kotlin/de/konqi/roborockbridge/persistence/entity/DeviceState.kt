package de.konqi.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.io.Serializable

enum class ProtocolMode {
    RO,
    RW
}

data class DeviceStateId(
    val device: String? = null,
    val schemaId: Int? = null
) : Serializable

@Entity
@Table(name = "device_state")
@IdClass(DeviceStateId::class)
data class DeviceState(
    @Id
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,
    @Id
    @Column(name = "schema_id")
    val schemaId: Int,
    val code: String,
    @Column(name = "current_value")
    val value: Int,
    @Enumerated(EnumType.STRING)
    val mode: ProtocolMode,
    val type: String,
    val property: String?
)
