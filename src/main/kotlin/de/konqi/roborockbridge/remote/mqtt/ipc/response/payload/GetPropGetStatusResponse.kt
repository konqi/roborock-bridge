package de.konqi.roborockbridge.remote.mqtt.ipc.response.payload

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

data class GetPropGetStatusResponse(
    @get:JsonProperty("msg_ver") val msgVer: Int,
    @get:JsonProperty("msg_seq") val msgSeq: Int,
    @get:JsonProperty("adbumper_status") val adbumperStatus: List<Int>,
    val events: List<String>,

    @JsonAnySetter
    @get:JsonAnyGetter
    val states: Map<String, Int> = mutableMapOf()
//    val state: Int,
//    val battery: Int,
//    @get:JsonProperty("clean_time") val cleanTime: Int,
//    @get:JsonProperty("clean_area") val cleanArea: Int,
//    @get:JsonProperty("error_code") val errorCode: Int,
//    @get:JsonProperty("map_present") val mapPresent: Int,
//    @get:JsonProperty("in_cleaning") val inCleaning: Int,
//    @get:JsonProperty("in_returning") val inReturning: Int,
//    @get:JsonProperty("in_fresh_state") val inFreshState: Int,
//    @get:JsonProperty("lab_status") val labStatus: Int,
//    @get:JsonProperty("water_box_status") val waterBoxStatus: Int,
//    @get:JsonProperty("fan_power") val fanPower: Int,
//    @get:JsonProperty("dnd_enabled") val dndEnabled: Int,
//    @get:JsonProperty("map_status") val mapStatus: Int,
//    @get:JsonProperty("is_locating") val isLocating: Int,
//    @get:JsonProperty("lock_status") val lockStatus: Int,
//    @get:JsonProperty("water_box_mode") val waterBoxMode: Int,
//    @get:JsonProperty("water_box_carriage_status") val waterBoxCarriageStatus: Int,
//    @get:JsonProperty("mop_forbidden_enable") val mopForbiddenEnable: Int,
//    @get:JsonProperty("camera_status") val cameraStatus: Int,
//    @get:JsonProperty("is_exploring") val isExploring: Int,
//    @get:JsonProperty("water_shortage_status") val waterShortageStatus: Int,
//    @get:JsonProperty("dock_type") val dockType: Int,
//    @get:JsonProperty("dust_collection_status") val dustCollectionStatus: Int,
//    @get:JsonProperty("auto_dust_collection") val autoDustCollection: Int,
//    @get:JsonProperty("avoid_count") val avoidCount: Int,
//    @get:JsonProperty("mop_mode") val mopMode: Int,
//    @get:JsonProperty("back_type") val backType: Int,
//    @get:JsonProperty("wash_phase") val washPhase: Int,
//    @get:JsonProperty("wash_ready") val washReady: Int,
//    @get:JsonProperty("wash_status") val washStatus: Int,
//    @get:JsonProperty("debug_mode") val debugMode: Int,
//    @get:JsonProperty("collision_avoid_status") val collisionAvoidStatus: Int,
//    @get:JsonProperty("switch_map_mode") val switchMapMode: Int,
//    @get:JsonProperty("dock_error_status") val dockErrorStatus: Int,
//    @get:JsonProperty("charge_status") val chargeStatus: Int,
//    @get:JsonProperty("unsave_map_reason") val unsaveMapReason: Int,
//    @get:JsonProperty("unsave_map_flag") val unsaveMapFlag: Int,
//    @get:JsonProperty("dry_status") val dryStatus: Int,
//    @get:JsonProperty("rdt") val rdt: Int,
//    @get:JsonProperty("clean_percent") val cleanPercent: Int,
//    @get:JsonProperty("rss") val rss: Int,
//    @get:JsonProperty("dss") val dss: Int,
//    @get:JsonProperty("common_status") val commonStatus: Int,
//    @get:JsonProperty("switch_status") val switchStatus: Int,
//    @get:JsonProperty("last_clean_t") val lastCleanTime: Int
)