package de.konqi.roborockbridge.roborockbridge.persistence.entity

import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["home_id", "device_key"])])
class Robot(
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Column(name = "device_id", nullable = false)
    val deviceId: String,
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
    @ElementCollection
    val state: Map<String, Long>,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)