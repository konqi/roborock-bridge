package de.konqi.roborockbridge.remote.helper

import de.konqi.roborockbridge.remote.mqtt.RequestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import java.util.*

class RequestMemoryTest {
    @BeforeEach
    fun beforeEach() {
        // let's assume there are three devices
        for (deviceId in 100..300 step 100) {
            // each of which has 3 open request
            for (requestId in 1..deviceId/100) {
                memory.put(
                    "device$deviceId", requestId, RequestData(
                        method = RequestMethod.GET_MAP_V1,
                        requestTimeMs = now - 1 - 1000 * requestId
                    )
                )
            }
        }
    }

//    @Test
//    fun `finds unresponsive devices`() {
//         // no device should be interpreted as unresponsive within a 10-second window
//        memory.findUnresponsiveDevices(10000).also {
//            assertThat(it).hasSize(0)
//        }
//
//        memory.findUnresponsiveDevices(3000).also {
//            assertThat(it).hasSize(1)
//            assertThat(it).containsOnly("device300")
//        }
//
//        memory.findUnresponsiveDevices(2000).also {
//            assertThat(it).hasSize(2)
//            assertThat(it).containsOnly("device300", "device200")
//        }
//
//        memory.findUnresponsiveDevices(1000).also {
//            assertThat(it).hasSize(3)
//            assertThat(it).containsOnly("device300", "device200", "device100")
//        }
//    }

    @Test
    fun `clears out old messages`() {
        assertThat(memory.clearMessagesOlderThan(10000)).hasSize(0)
        assertThat(memory.clearMessagesOlderThan(3000)).hasSize(1)
        assertThat(memory.clearMessagesOlderThan(2000)).hasSize(2)
        assertThat(memory.clearMessagesOlderThan(1000)).hasSize(3)
        // there should be nothing left
        assertThat(memory.clearMessagesOlderThan(0)).hasSize(0)
    }

    companion object {
        private val memory = spy(RequestMemory())
        private val now = Date().time

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            doReturn(now).`when`(memory).now

            assertThat(memory.now).isEqualTo(now)
        }

    }
}