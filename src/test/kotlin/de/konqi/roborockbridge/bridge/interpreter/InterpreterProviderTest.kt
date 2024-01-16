package de.konqi.roborockbridge.bridge.interpreter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.konqi.roborockbridge.persistence.DataAccessLayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [InterpreterProvider::class, GenericInterpreter::class, S8UltraInterpreter::class])
class InterpreterProviderTest {
    @MockBean
    lateinit var dataAccessLayer: DataAccessLayer

    @Autowired
    lateinit var interpreterProvider: InterpreterProvider

    @Test
    fun getInterpreterForModel() {
        assertThat(interpreterProvider.getInterpreterForModel("roborock.vacuum.a70")).isInstanceOf(S8UltraInterpreter::class.java)
        assertThat(interpreterProvider.getInterpreterForModel("*")).isInstanceOf(GenericInterpreter::class.java)
        assertThat(interpreterProvider.getInterpreterForModel("completely unknown string")).isInstanceOf(GenericInterpreter::class.java)
    }

    @Test
    fun testPreprocessor() {
        // given a map-ish object
        val mapish = """{"ignore_me":123,"fan_power": "quiet", "water_box_mode": "OFF"}"""
        // and an interpreter
        val interpreter = interpreterProvider.getInterpreterForModel("roborock.vacuum.a70")!!

        // the preprocessor should convert string values to their corresponding int value
        val preprocessed = interpreter.preprocessMapNode(jacksonObjectMapper().readTree(mapish))

        assertThat(preprocessed.get("fan_power").numberValue()).isEqualTo(101)
        assertThat(preprocessed.get("water_box_mode").numberValue()).isEqualTo(200)
        assertThat(preprocessed.get("ignore_me").numberValue()).isEqualTo(123)
    }
}