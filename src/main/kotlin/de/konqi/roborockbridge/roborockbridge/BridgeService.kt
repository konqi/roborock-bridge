package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.protocol.RoborockRestApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

@Service
class BridgeService(@Autowired val roborockRestApi: RoborockRestApi) {

    @EventListener(ApplicationReadyEvent::class)
    fun worker() {
        if (!roborockRestApi.isLoggedIn) {
            if (roborockRestApi.canLogIn) {
                roborockRestApi.login()
                roborockRestApi.getHomeDetail()
            } else {
                logger.error("Missing configuration. Exiting.")
                exitProcess(-1)
            }
        }

//        roborockRestApi.getUserHome()
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}