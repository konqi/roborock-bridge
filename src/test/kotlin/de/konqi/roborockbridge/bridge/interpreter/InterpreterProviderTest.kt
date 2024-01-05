package de.konqi.roborockbridge.bridge.interpreter

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
}