package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.protocol.RoborockMqtt
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class BridgeService() : DisposableBean {
//    @Autowired
//    lateinit var roborockRestApi: RoborockRestApi

    @Autowired
    lateinit var roborockData: RoborockData

    @Autowired
    lateinit var mqttClient: RoborockMqtt

    private var run = true

    @EventListener(ApplicationReadyEvent::class)
    fun worker() {
//        if (!roborockRestApi.isLoggedIn) {
//            if (roborockRestApi.canLogIn) {
//                roborockRestApi.login()
//                roborockRestApi.getHomeDetail()
//            } else {
//                logger.error("Missing configuration. Exiting.")
//                exitProcess(-1)
//            }
//        }
//
//        val (home, robots) = roborockRestApi.getUserHome()
//        roborockData.home = home
//        roborockData.robots = robots
//        roborockData.rriot = roborockRestApi.rriot

        mqttClient.connect()

        val deviceId = roborockData.robots[0].deviceId
        val deviceLocalKey = roborockData.robots.first { it.deviceId == deviceId }.deviceInformation.localKey
        mqttClient.monitorDevice(deviceId = deviceId, key = deviceLocalKey)

//        while (run) {
//        println("sleep 1000ms")
//        Thread.sleep(1000)
//        mqttClient.publishStatusRequest(deviceId = deviceId, deviceLocalKey = deviceLocalKey)

//        println("sleep 5000ms (wait for response)")
//        Thread.sleep(5000)
//        println("sleep ended")
//        }
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