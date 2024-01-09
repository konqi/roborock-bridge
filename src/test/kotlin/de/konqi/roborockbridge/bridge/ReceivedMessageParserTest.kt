package de.konqi.roborockbridge.bridge

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppSegmentCleanRequestDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.AppStartDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.SetCleanMotorModeDTO
import de.konqi.roborockbridge.remote.mqtt.ipc.request.payload.StringDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestBeanProvider::class, ReceivedMessageParser::class, ReceivedMessageParserTest.Companion.Config::class])
@ActiveProfiles("bridge")
class ReceivedMessageParserTest {
    @Autowired
    lateinit var parser: ReceivedMessageParser

    @Test
    fun `invalid payloads don't crash the parser`() {
        val msg = parser.parse("topic", "Yo!".toByteArray())
        assertThat(msg).isNull()
    }

    @Test
    fun `malformed payloads don't crash the parser`() {
        assertDoesNotThrow { parser.parse("topic", "{Yo!".toByteArray()) }
        assertDoesNotThrow { parser.parse("topic", "{{[Yo!}".toByteArray()) }
    }

    @Test
    fun `works for simple payloads`() {
        val getMapRequest = parser.parse("/home/123/device/234/get", "map".toByteArray())
        assertThat(getMapRequest?.header?.targetType).isEqualTo(TargetType.DEVICE)
        assertThat(getMapRequest?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.MAP)

        val getStateRequest = parser.parse("/home/123/device/234/get", "state".toByteArray())
        assertThat(getStateRequest?.header?.targetType).isEqualTo(TargetType.DEVICE)
        assertThat(getStateRequest?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.STATE)
    }

    @Test
    fun `set a device property`(){
        val setFanSpeedRequest = parser.parse("/home/123/device/234/fan_power/set", "120".toByteArray())
        assertThat(setFanSpeedRequest?.header?.targetType).isEqualTo(TargetType.DEVICE_PROPERTY)
        assertThat(setFanSpeedRequest?.header?.homeId).isEqualTo(123)
        assertThat(setFanSpeedRequest?.header?.deviceId).isEqualTo("234")
        assertThat(setFanSpeedRequest?.header?.targetIdentifier).isEqualTo("fan_power")
        assertThat(setFanSpeedRequest?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.UNKNOWN)
        assertThat((setFanSpeedRequest?.body?.parameters as? StringDTO)?.value).isEqualTo("120")
    }

    @Test
    fun `works with json payload for mode selection`() {
        val msg = parser.parse(
            "/home/123/device/234/action", """{
            |"action": "clean_mode",
            |"fan_power": 1,
            |"mop_mode": 2,
            |"water_box_mode": 3
            |}""".trimMargin().toByteArray()
        )

        assertThat(msg?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.CLEAN_MODE)
        assertThat(msg?.body?.parameters).isInstanceOf(SetCleanMotorModeDTO::class.java)
        val dto = msg?.body?.parameters as SetCleanMotorModeDTO
        assertThat(dto.mopMode).isEqualTo(2)
        assertThat(dto.fanPower).isEqualTo(1)
        assertThat(dto.waterBoxMode).isEqualTo(3)
    }

    @Test
    fun `works with json payload for segment clean`() {
        val msg = parser.parse(
            "/home/123/device/234/action", """{
            |"action": "segments",
            |"clean_mop": 0, 
            |"clean_order_mode": 0,
            |"repeat": 1,
            |"segments": [1,2,3]
            |}""".trimMargin().toByteArray()
        )

        assertThat(msg?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.SEGMENTS)
        assertThat(msg?.body?.parameters).isInstanceOf(AppSegmentCleanRequestDTO::class.java)
        val dto = msg?.body?.parameters as AppSegmentCleanRequestDTO
        assertThat(dto.cleanMop).isEqualTo(0)
        assertThat(dto.cleanOrderMode).isEqualTo(0)
        assertThat(dto.repeat).isEqualTo(1)
        assertThat(dto.segments).contains(1, 2, 3)
    }

    @Test
    fun `incomplete json payloads are okay`() {
        val msg = parser.parse(
            "/home/123/device/234/action", """{
            |"action": "clean_mode",
            |"fan_power": 1
            |}""".trimMargin().toByteArray()
        )

        assertThat(msg?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.CLEAN_MODE)
        assertThat(msg?.body?.parameters).isInstanceOf(SetCleanMotorModeDTO::class.java)
        val dto = msg?.body?.parameters as SetCleanMotorModeDTO
        assertThat(dto.fanPower).isEqualTo(1)
        // others should have default values
        assertThat(dto.mopMode).isEqualTo(0)
        assertThat(dto.waterBoxMode).isEqualTo(0)
    }

    @Test
    fun `can handle missing, but optional, payload`() {
        val msg = parser.parse(
            "/home/123/device/234/action", "start".toByteArray()
        )

        assertThat(msg?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.START)
        assertThat(msg?.body?.parameters).isInstanceOf(AppStartDTO::class.java)
            .extracting("cleanMop").isEqualTo(0)
    }

    @Test
    fun `can handle optional payload`() {
        val msg = parser.parse(
            "/home/123/device/234/action", """{"action":"start","clean_mop": 1}""".toByteArray()
        )

        assertThat(msg?.body?.actionKeyword).isEqualTo(ActionKeywordsEnum.START)
        assertThat(msg?.body?.parameters).isInstanceOf(AppStartDTO::class.java)
            .extracting("cleanMop").isEqualTo(1)
    }

    @Nested
    @DisplayName("regex")
    inner class RegexTests {
        @Test
        fun `can parse device get`() {
            ReceivedMessageHeader.deviceIdExtractionRegex.find("/home/123/device/234/get").also {
                assertThat(it?.groups?.get("home")?.value).isEqualTo("123")
                assertThat(it?.groups?.get("device")?.value).isEqualTo("234")
                assertThat(it?.groups?.get("command")?.value).isEqualTo("get")
            }
        }

        @Test
        fun `can parse device action`() {
            ReceivedMessageHeader.deviceIdExtractionRegex.find("/home/123/device/234/action").also {
                assertThat(it?.groups?.get("home")?.value).isEqualTo("123")
                assertThat(it?.groups?.get("device")?.value).isEqualTo("234")
                assertThat(it?.groups?.get("command")?.value).isEqualTo("action")
            }
        }

        @Test
        fun `can parse routine action`() {
            ReceivedMessageHeader.deviceIdExtractionRegex.find("/home/123/routine/345/action").also {
                assertThat(it?.groups?.get("home")?.value).isEqualTo("123")
                assertThat(it?.groups?.get("routine")?.value).isEqualTo("345")
                assertThat(it?.groups?.get("command")?.value).isEqualTo("action")
            }
        }

        @Test
        fun `can parse device property set`() {
            ReceivedMessageHeader.deviceIdExtractionRegex.find("/home/123/device/234/fan_power/set").also {
                assertThat(it?.groups?.get("home")?.value).isEqualTo("123")
                assertThat(it?.groups?.get("device")?.value).isEqualTo("234")
                assertThat(it?.groups?.get("property")?.value).isEqualTo("fan_power")
            }
        }
    }

    companion object {
        @TestConfiguration
        class Config {
            @Bean
            fun mockMqttConfig(): BridgeMqttConfig = Mockito.mock(BridgeMqttConfig::class.java).also {
                `when`(it.baseTopic).thenReturn("")
            }
        }
    }
}