package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.roborockbridge.protocol.helper.DeviceKeyMemory
import de.konqi.roborockbridge.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.Request101Factory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [RoborockMqtt::class, RequestMemory::class, DeviceKeyMemory::class, MessageDecoder::class, Request101Factory::class, TestBeanProvider::class])
class RoborockMqttTest(@Autowired val roborockMqtt: RoborockMqtt) {

    @Test
    fun publishReturnToChargingStation() {
        roborockMqtt.publishReturnToChargingStation("123")
    }
}