package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.protocol.RoborockMqtt
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockRestApi
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.exitProcess

@Service
class BridgeService(@Autowired val roborockRestApi: RoborockRestApi) : DisposableBean {
    private var run = true
    private lateinit var mqttClient: RoborockMqtt

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

        val (home, robots) = roborockRestApi.getUserHome()

        mqttClient = RoborockMqtt(roborockRestApi.rriot)
        mqttClient.connect()
        println("connect")

        while (run) {
            println("sleep 1000ms")
            Thread.sleep(1000)
        }
    }

    override fun destroy() {
        mqttClient.disconnect()
        println("destroy")
        run = false
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}