package de.konqi.roborockbridge.remote.rest.dto.homedetail

data class Device(
    val duid: String,
    val name: String,
    val attribute: String?,
    val activeTime: Long,
    val localKey: String,
    val runtimeEnv: String?,
    val timeZoneId: String,
    val iconUrl: String,
    val productId: String,
    // probably decimal
    val lon: String?,
    // probably decimal
    val lat: String?,
    val share: Boolean,
    // no idea
    val shareTime: String?,
    val online: Boolean,
    val fv: String,
    val pv: String,
    val roomId: String?,
    val tuyaUuid: String?,
    val tuyaMigrated: Boolean,
    // unknown
    val extra: String?,
    // unknown
    val setting: String?,
    val sn: String,
    val featureSet: String,
    val newFeatureSet: String,
    val deviceStatus: Map<String, Int>,
    val silentOtaSwitch: Boolean,
    val f: Boolean,
)