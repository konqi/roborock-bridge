package de.konqi.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.util.*

enum class ProtocolMode {
    RO,
    RW
}

data class DeviceStateId(
    val device: String? = null,
    val code: String? = null
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
    val code: String,
    @Column(name = "modified_date")
    @field:UpdateTimestamp
    val modifiedDate: Date = Date(),
    @Column(name = "current_value")
    val value: Int,
    @Column(name = "schema_id")
    val schemaId: String? = null,
    @Enumerated(EnumType.STRING)
    val mode: ProtocolMode? = null,
    val type: String? = null,
    val property: String? = null,
)
