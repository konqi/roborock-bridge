package de.konqi.roborockbridge.roborockbridge.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

enum class ProtocolMode {
    RO,
    RW
}

@Embeddable
data class RobotState(
    @Column()
    val protocolId: Int,
    val code: String,
    @Column(name = "current_value")
    val value: Int,
    @Enumerated(EnumType.STRING)
    val mode: ProtocolMode,
    val type: String,
    val property: String?
)
