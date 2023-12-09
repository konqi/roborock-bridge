package de.konqi.roborockbridge.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class Home(
    @Id
    @Column(name = "home_id")
    var homeId: Int,
    @Column(nullable = false)
    var name: String
)