package de.konqi.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["home_id", "device_id"])])
data class Device(
    @Id
    @Column(name = "device_id", nullable = false)
    val deviceId: String,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Column(name = "device_key", nullable = false)
    val deviceKey: String,
    @Column(nullable = false)
    val name: String,
    @Column(name = "product_name", nullable = false)
    val productName: String,
    @Column(nullable = false)
    val model: String,
    @Column(nullable = false)
    val firmwareVersion: String,
    @Column(nullable = false)
    val serialNumber: String,
    @OneToMany(mappedBy = "device", cascade = [CascadeType.REMOVE], fetch = FetchType.EAGER)
    val state: List<DeviceState> = emptyList()
)