package de.konqi.roborockbridge.protocol

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.protocol.helper.RequestMemory
import de.konqi.roborockbridge.protocol.mqtt.MessageDecoder
import de.konqi.roborockbridge.protocol.mqtt.Request101Factory
import de.konqi.roborockbridge.protocol.mqtt.RequestMethod
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [RoborockMqtt::class, RequestMemory::class, MessageDecoder::class, Request101Factory::class, TestBeanProvider::class])
class RoborockMqttTest(@Autowired val roborockMqtt: RoborockMqtt) {

    @Test
    fun publishReturnToChargingStation() {
        roborockMqtt.publishRequest("123", RequestMethod.APP_CHARGE)
    }
}