package de.konqi.roborockbridge.bridge.dto

import de.konqi.roborockbridge.persistence.entity.Home

data class HomeForPublish(
    var homeId: Int,
    var name: String
) {
    companion object {
        fun fromHomeEntity(home:Home) =
            HomeForPublish(
                homeId = home.homeId,
                name = home.name
            )

    }
}