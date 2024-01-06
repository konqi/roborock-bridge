package de.konqi.roborockbridge.remote.mqtt.ipc.request.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// app_segment_clean [{"clean_mop":0,"clean_order_mode":0,"repeat":1,"segments":[18]}]
@JsonIgnoreProperties(ignoreUnknown = true)
data class AppSegmentCleanRequestDTO(
    @get:JsonProperty("clean_mop")
    var cleanMop: Int = 0,
    @get:JsonProperty("clean_order_mode")
    var cleanOrderMode: Int = 0,
    @get:JsonProperty("repeat")
    var repeat: Int = 1,
    @get:JsonProperty("segments")
    var segments: List<Int> = mutableListOf()
) : IpcRequestDTO