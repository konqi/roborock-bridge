package de.konqi.roborockbridge.bridge

import org.junit.ClassRule
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.hivemq.HiveMQContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractMqttTest {
    companion object {
        private val HIVEMQ_IMAGE = DockerImageName
            .parse("hivemq/hivemq-ce")


        @JvmField
        @ClassRule
        val mqttBroker = HiveMQContainer(HIVEMQ_IMAGE) // .withoutPrepackagedExtensions()

        @JvmStatic
        @BeforeAll
        fun startMockserver(): Unit {
            mqttBroker.start()
        }
    }
}