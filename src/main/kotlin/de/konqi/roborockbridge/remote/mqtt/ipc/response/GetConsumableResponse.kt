package de.konqi.roborockbridge.remote.mqtt.ipc.response

import com.fasterxml.jackson.annotation.JsonProperty

data class GetConsumableResponse(
    @get:JsonProperty("main_brush_work_time") val mainBrushWorkTime: Int,
    @get:JsonProperty("side_brush_work_time") val sideBrushWorkTime: Int,
    @get:JsonProperty("filter_work_time") val filterWorkTime: Int,
    @get:JsonProperty("filter_element_work_time") val filterElementWorkTime: Int,
    @get:JsonProperty("sensor_dirty_time") val sensorDirtyTime: Int,
    @get:JsonProperty("strainer_work_times") val strainerWorkTimes: Int,
    @get:JsonProperty("dust_collection_work_times") val dustCollectionWorkTimes: Int,
    @get:JsonProperty("cleaning_brush_work_times") val cleaningBrushWorkTimes: Int,
)
