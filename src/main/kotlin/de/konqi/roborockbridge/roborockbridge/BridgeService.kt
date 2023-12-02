package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.persistence.HomeRepository
import de.konqi.roborockbridge.roborockbridge.persistence.RobotRepository
import de.konqi.roborockbridge.roborockbridge.persistence.RoomRepository
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Home
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Robot
import de.konqi.roborockbridge.roborockbridge.persistence.entity.Room
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockCredentials
import de.konqi.roborockbridge.roborockbridge.protocol.RoborockMqtt
import de.konqi.roborockbridge.roborockbridge.protocol.rest.HomeApi
import de.konqi.roborockbridge.roborockbridge.protocol.rest.LoginApi
import de.konqi.roborockbridge.roborockbridge.protocol.rest.UserApi
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
//@Profile("off")
class BridgeService(
    @Autowired private val mqttClient: RoborockMqtt,
    @Autowired private val loginApi: LoginApi,
    @Autowired private val homeApi: HomeApi,
    @Autowired private val userApi: UserApi,
    @Autowired private val roborockCredentials: RoborockCredentials,
    @Autowired private val bridgeMqtt: BridgeMqtt,
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository,
    private val robotRepository: RobotRepository
) {

    private var run = true

    fun init() {
        if (!roborockCredentials.isLoggedIn) {
            val loginData = loginApi.login()
            roborockCredentials.fromRriot(loginData.rriot, loginData.token)
        }

        val home = homeApi.getHome()
        val homeEntity = homeRepository.save(Home(homeId = home.rrHomeId, name = home.name))

        val homeDetails = userApi.getUserHome(homeEntity.homeId)

        val rooms = roomRepository.saveAll(
            homeDetails.rooms.map { Room(home = homeEntity, roomId = it.id, name = it.name) }
        )

        val robots = homeDetails.devices.map { device ->
            val product = homeDetails.products.find { product -> product.id == device.productId }
                ?: throw RuntimeException("Unable to resolve product information for product id '${device.productId}'")

            val status: Map<String, Long> =
                device.deviceStatus.map { status -> product.schema.find { it.id.toInt() == status.key }?.code to status.value }
                    .filter { it.first != null }
                    .filterIsInstance<Pair<String, Long>>()
                    .toMap()

            Robot(
                home = homeEntity,
                deviceId = device.duid,
                name = device.name,
                deviceKey = device.localKey,
                productName = product.name,
                model = product.model,
                firmwareVersion = device.fv,
                serialNumber = device.sn,
                state = status
            )
        }.run { robotRepository.saveAll(this) }

        // announce devices on mqtt broker
        bridgeMqtt.announceHome(homeEntity)
        robots.forEach(bridgeMqtt::announceDevice)
        bridgeMqtt.announceRooms(rooms.toList())
    }

    @EventListener(ApplicationReadyEvent::class)
    fun worker() {
        while (!bridgeMqtt.mqttClient.isConnected) {
            Thread.sleep(1000)
        }
        init()
//        mqttClient.connect()
//
//        val deviceId = roborockData.robots[0].deviceId
//        val deviceLocalKey = roborockData.robots.first { it.deviceId == deviceId }.deviceInformation.localKey
//        mqttClient.monitorDevice(deviceId = deviceId, key = deviceLocalKey)

//        while (run) {
//        println("sleep 1000ms")
//        Thread.sleep(1000)
//        mqttClient.publishStatusRequest(deviceId = deviceId, deviceLocalKey = deviceLocalKey)

//        println("sleep 5000ms (wait for response)")
//        Thread.sleep(5000)
//        println("sleep ended")
//        }
    }

    @PreDestroy
    fun shutdown() {
//        mqttClient.disconnect()
        println("destroy")
        run = false
    }

    companion object {
        private val logger by LoggerDelegate()
    }
}