package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppSegmentCleanRequestDTO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.list
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.awaitility.Durations.TEN_SECONDS
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource


@SpringBootTest(classes = [BridgeMqtt::class, ReceivedMessageParser::class, TestBeanProvider::class])
@ActiveProfiles("bridge")
class BridgeMqttTest : AbstractMqttTest() {
    @Autowired
    lateinit var bridgeMqtt: BridgeMqtt

    @Test
    fun `bridge is connected`() {
        assertThat(bridgeMqtt.mqttClient.isConnected).isTrue()
    }

    @Test
    fun `receives routine message`() {
        mqttClient.publish("/home/$HOME_ID/routine/$ROUTINE_ID/action", byteArrayOf(), 0, false)

        await atMost TEN_SECONDS untilCallTo { bridgeMqtt.inboundMessagesQueue } has {
            size > 0
        }
        assertThat(bridgeMqtt.inboundMessagesQueue.size).isGreaterThan(0)
        val message = bridgeMqtt.inboundMessagesQueue.remove()
        assertThat(message.header.command).isEqualTo(CommandType.ACTION)
        assertThat(message.header.targetType).isEqualTo(TargetType.ROUTINE)
        assertThat(message.header.targetIdentifier).isEqualTo(ROUTINE_ID.toString())
        assertThat(message.header.homeId).isEqualTo(HOME_ID)
    }

    @Test
    fun `receives return to home action message`() {
        mqttClient.publish("/home/$HOME_ID/device/$DEVICE_ID/action", "home".toByteArray(), 0, false)

        await atMost TEN_SECONDS untilCallTo { bridgeMqtt.inboundMessagesQueue } has {
            size > 0
        }
        assertThat(bridgeMqtt.inboundMessagesQueue.size).isGreaterThan(0)
        val message = bridgeMqtt.inboundMessagesQueue.remove()
        assertThat(message.header.command).isEqualTo(CommandType.ACTION)
        assertThat(message.header.targetType).isEqualTo(TargetType.DEVICE)
        assertThat(message.header.targetIdentifier).isEqualTo(DEVICE_ID.toString())
        assertThat(message.header.homeId).isEqualTo(HOME_ID)
        assertThat(message.body.actionKeyword).isEqualTo(ActionKeywordsEnum.HOME)
    }

    @Test
    fun `receives segment clean action message`() {
        mqttClient.publish(
            "/home/$HOME_ID/device/$DEVICE_ID/action", """
            {
            "action": "segments",
            "segments": [18,19,20]
            }
        """.trimIndent().toByteArray(), 0, false
        )

        await atMost TEN_SECONDS untilCallTo { bridgeMqtt.inboundMessagesQueue } has {
            size > 0
        }
        assertThat(bridgeMqtt.inboundMessagesQueue.size).isGreaterThan(0)
        val message = bridgeMqtt.inboundMessagesQueue.remove()
        assertThat(message.header)
            .hasFieldOrPropertyWithValue("command", CommandType.ACTION)
            .hasFieldOrPropertyWithValue("targetType", TargetType.DEVICE)
            .hasFieldOrPropertyWithValue("targetIdentifier", DEVICE_ID.toString())
            .hasFieldOrPropertyWithValue("homeId", HOME_ID)
        assertThat(message.body)
            .hasFieldOrPropertyWithValue("actionKeyword", ActionKeywordsEnum.SEGMENTS)
            .extracting("parameters").asInstanceOf(type(AppSegmentCleanRequestDTO::class.java))
            .extracting("segments").asInstanceOf(list(Int::class.java))
            .hasSameElementsAs(listOf(18, 19, 20))
    }

    @ParameterizedTest
    @ValueSource(strings = ["map", "state"])
    fun `receives get map message`(what: String) {
        mqttClient.publish("/home/$HOME_ID/device/$DEVICE_ID/get", what.toByteArray(), 0, false)

        await atMost TEN_SECONDS untilCallTo { bridgeMqtt.inboundMessagesQueue } has {
            size > 0
        }
        assertThat(bridgeMqtt.inboundMessagesQueue.size).isGreaterThan(0)
        val message = bridgeMqtt.inboundMessagesQueue.remove()
        assertThat(message.header)
            .hasFieldOrPropertyWithValue("command", CommandType.GET)
            .hasFieldOrPropertyWithValue("targetType", TargetType.DEVICE)
            .hasFieldOrPropertyWithValue("targetIdentifier", DEVICE_ID.toString())
            .hasFieldOrPropertyWithValue("homeId", HOME_ID)
        assertThat(message.body.actionKeyword).isEqualTo(ActionKeywordsEnum.fromValue(what))
    }

    companion object {
        const val HOME_ID = 123
        const val ROUTINE_ID = 987
        const val DEVICE_ID = 456

        lateinit var mqttClient: MqttClient

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bridge-mqtt.url") { "tcp://${mqttBroker.host}:${mqttBroker.mqttPort}" }
            registry.add("bridge-mqtt.client_id") { "integration-test" }
            registry.add("bridge-mqtt.base_topic") { "" }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mqttClient = MqttClient("tcp://${mqttBroker.host}:${mqttBroker.mqttPort}", "test-publisher", null)
            mqttClient.connect(MqttConnectOptions().apply {
                isCleanSession = true
            })
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            mqttClient.disconnect()
        }
    }
}