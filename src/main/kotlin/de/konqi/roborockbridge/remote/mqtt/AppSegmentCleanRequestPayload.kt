package de.konqi.roborockbridge.remote.mqtt

import com.fasterxml.jackson.annotation.JsonProperty

// app_segment_clean [{"clean_mop":0,"clean_order_mode":0,"repeat":1,"segments":[18]}]
data class AppSegmentCleanRequestPayload(
    @get:JsonProperty("clean_mop")
    val cleanMop: Int,
    @get:JsonProperty("clean_order_mode")
    val cleanOrderMode: Int,
    @get:JsonProperty("repeat")
    val repeat: Int,
    @get:JsonProperty("segments")
    val segments: List<Int>
)
