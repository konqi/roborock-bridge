package de.konqi.roborockbridge.roborockbridge.protocol.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.konqi.roborockbridge.roborockbridge.protocol.mqtt.ipc.response.IpcResponseWrapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Protocol102WrapperTest {
    @Test
    fun deserializationTest() {
        val serialized =
            """{"t":1700154301,"dps":{"102":"{\"id\":1,\"result\":[{\"msg_ver\":2,\"msg_seq\":619,\"state\":8,\"battery\":100,\"clean_time\":4203,\"clean_area\":70492500,\"error_code\":0,\"map_present\":1,\"in_cleaning\":0,\"in_returning\":0,\"in_fresh_state\":1,\"lab_status\":1,\"water_box_status\":1,\"fan_power\":103,\"dnd_enabled\":0,\"map_status\":3,\"is_locating\":0,\"lock_status\":0,\"water_box_mode\":203,\"water_box_carriage_status\":1,\"mop_forbidden_enable\":1,\"camera_status\":385,\"is_exploring\":0,\"adbumper_status\":[0,0,0],\"water_shortage_status\":0,\"dock_type\":7,\"dust_collection_status\":0,\"auto_dust_collection\":1,\"avoid_count\":119,\"mop_mode\":300,\"back_type\":-1,\"wash_phase\":0,\"wash_ready\":0,\"wash_status\":0,\"debug_mode\":0,\"collision_avoid_status\":1,\"switch_map_mode\":0,\"dock_error_status\":0,\"charge_status\":1,\"unsave_map_reason\":0,\"unsave_map_flag\":0,\"dry_status\":0,\"rdt\":0,\"clean_percent\":0,\"rss\":2,\"dss\":2713,\"common_status\":2,\"events\":[],\"switch_status\":0,\"last_clean_t\":1699791825}]}"}}"""
        val protocol102Wrapper: IpcResponseWrapper = objectMapper.readValue(serialized)
        assertTrue(protocol102Wrapper.dps.keys.contains("102"))
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}