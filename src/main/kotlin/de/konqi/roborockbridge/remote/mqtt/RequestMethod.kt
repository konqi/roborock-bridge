package de.konqi.roborockbridge.remote.mqtt

import com.fasterxml.jackson.databind.JsonNode
import de.konqi.roborockbridge.remote.mqtt.ipc.response.GetConsumableResponse
import de.konqi.roborockbridge.remote.mqtt.ipc.response.GetPropGetStatusResponse
import de.konqi.roborockbridge.remote.mqtt.ipc.response.RoomMapping
import kotlin.reflect.KClass

enum class RequestMethod(val value: String, val decodesTo: KClass<*> = JsonNode::class) {
    GET_PROP("get_prop", Array<GetPropGetStatusResponse>::class), // "params":["get_status"]
    GET_MAP_V1("get_map_v1", Array<String>::class),
    APP_GET_INIT_STATUS("app_get_init_status"),
    GET_SERIAL_NUMBER("get_serial_number"),
    SET_APP_TIMEZONE("set_app_timezone", Array<String>::class), // "params":["Europe/Berlin",2]
    GET_SCENES_VALID_TIDS("get_scenes_valid_tids"),
    GET_SERVER_TIMER("get_server_timer"),
    GET_ROOM_MAPPING("get_room_mapping", Array<RoomMapping>::class),
    GET_CUSTOMIZE_CLEAN_MODE("get_customize_clean_mode"),
    GET_CLEAN_SEQUENCE("get_clean_sequence"),
    GET_MULTI_MAPS_LIST("get_multi_maps_list"),
    SET_FDS_ENDPOINT("set_fds_endpoint", Array<String>::class), // "params":["-eu-1316693915.cos.eu-frankfurt.myqcloud.com"]
    ENABLE_LOG_UPLOAD("enable_log_upload", Array<String>::class), // "params":[9,3]
    GET_CONSUMABLE("get_consumable", Array<GetConsumableResponse>::class),
    GET_TIMEZONE("get_timezone", Array<String>::class),
    GET_CURRENT_SOUND("get_current_sound"),
    GET_CARPET_CLEAN_MODE("get_carpet_clean_mode"),
    APP_SEGMENT_CLEAN("app_segment_clean"),
    SET_CLEAN_MOTOR_MODE("set_clean_motor_mode", Array<String>::class),
    SET_CUSTOM_MODE("set_custom_mode", Array<String>::class),

    // APP_STAT example payloads:
    // "params":[{"data":[{"data":[{"id":10404,"times":[1700310530]}],"type":0},{"data":[{"durations":[[1545,1700140184]],"id":20000}],"type":1}],"ver":"0.1"}]
    // "params":[{"data":[{"data":{"appType":"roborock","language":"en_US","mcc":"not-cn","mnc":"*","mobileBrand":"*","mobileModel":"sdk_gphone64_arm64","os":"android","osVersion":"33","pluginVersion":"3426","region":"Europe/Berlin"},"times":[1700310528],"type":2}]
    APP_STAT("app_stat", Array<String>::class),
    APP_CHARGE("app_charge", Array<String>::class),

    SET_COLLISION_AVOID_STATUS("set_collision_avoid_status", Array<String>::class), // {"status":1}
    GET_LED_STATUS("get_led_status", Array<Int>::class),
    GET_CHILD_LOCK_STATUS("get_child_lock_status"),
    GET_VALLEY_ELECTRICITY_TIMER("get_valley_electricity_timer"),
    GET_DND_TIMER("get_dnd_timer"),
    APP_PAUSE("app_pause", Array<String>::class),
    GET_SOUND_PROGRESS("get_sound_progress"),
    GET_SOUND_VOLUME("get_sound_volume", Array<Int>::class),
    SET_CARPET_CLEAN_MODE("set_carpet_clean_mode", Array<String>::class), // {"carpet_clean_mode":0}
    APP_GET_CARPET_DEEP_CLEAN_STATUS("app_get_carpet_deep_clean_status", JsonNode::class),
    GET_CARPET_MODE("get_carpet_mode"),
}