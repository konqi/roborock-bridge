package de.konqi.roborockbridge.roborockbridge.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["home_id", "room_id"])])
class Room(
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "home_id", nullable = false)
    val home: Home,
    @Column(name = "room_id", nullable = false)
    val roomId: Int,
    @Column(nullable = false)
    val name: String,
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)