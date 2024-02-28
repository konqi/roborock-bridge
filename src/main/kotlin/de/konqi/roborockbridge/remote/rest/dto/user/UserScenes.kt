package de.konqi.roborockbridge.remote.rest.dto.user

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import de.konqi.roborockbridge.utility.NestedJsonDeserializer
import de.konqi.roborockbridge.utility.ObjectMapperDelegate
import java.io.IOException

internal class UserSchemasParamDeserializer : NestedJsonDeserializer<UserSceneParam>(UserSceneParam::class.java)

data class UserScenes(
    val id: Int,
    val name: String,
    @JsonDeserialize(converter = UserSchemasParamDeserializer::class)
    val param: UserSceneParam,
    val enabled: Boolean,
    val extra: String?,
    val type: String
)

data class UserSceneParam(
    val triggers: List<JsonNode>,
    val action: UserSceneAction,
    val matchType: String
)

data class UserSceneAction(
    val type: String,
    val items: List<UserSceneParamActionItem>
)

data class UserSceneParamActionItem(
    val id: Int,
    val type: String,
    val name: String,
    val entityId: String,
    val param: ActionItemParam<Any>,
    val finishDpIds: List<Int>
)


class ActionItemParamDeserializer : JsonDeserializer<ActionItemParam<*>?>() {
    @Throws(IOException::class)
    override fun deserialize(
        jsonParser: JsonParser?,
        deserializationContext: DeserializationContext
    ): ActionItemParam<*> {
        val value = jsonParser?.valueAsString ?: ""
        val node: JsonNode = objectMapper.readValue(value)
        val id: Int = node["id"].asInt()
        val method: String = node["method"].asText() ?: ""

        return when (method) {
            "do_scenes_segments" -> {
                val params: DifferentiatingCleanupParams<ParamsForSegmentedCleanup> =
                    objectMapper.treeToValue(node["params"])
                ActionItemParam(id = id, method = method, params = params)
            }

            "do_scenes_app_start" -> {
                val params: List<ScenesAppStartParam> =
                    objectMapper.treeToValue(node["params"])
                ActionItemParam(id = id, method = method, params = params)
            }

            "do_scenes_zones" -> {
                val params: DifferentiatingCleanupParams<ParamsForZonedCleanup> =
                    objectMapper.treeToValue(node["params"])
                ActionItemParam(id = id, method = method, params = params)
            }

            else -> {
                throw RuntimeException("Unknown method $method")
            }
        }
    }

    companion object {
        private val objectMapper by ObjectMapperDelegate()
    }
}


@JsonDeserialize(using = ActionItemParamDeserializer::class)
data class ActionItemParam<T>(
    val id: Int,
    val method: String,
    val params: T
)

interface BaseCleanupParams {
    @get:JsonProperty("fan_power")
    val fanPower: Int

    @get:JsonProperty("water_box_mode")
    val waterBoxMode: Int

    @get:JsonProperty("mop_mode")
    val mopMode: Int

    @get:JsonProperty("mop_template_id")
    val mopTemplateId: Int
    val repeat: Int
}

interface ExtendedCleanupParams : BaseCleanupParams {
    @get:JsonProperty("clean_order_mode")
    val cleanOrderMode: Int

    @get:JsonProperty("map_flag")
    val mapFlag: Int
    val tid: String
}

data class ScenesAppStartParam(
    val source: Int,
    override val fanPower: Int,
    override val waterBoxMode: Int,
    override val mopMode: Int,
    override val mopTemplateId: Int,
    override val repeat: Int
) : BaseCleanupParams

data class DifferentiatingCleanupParams<T>(
    val data: List<T>,
    val source: Int
)

data class ParamsForZonedCleanup(
    val zones: List<Zone>,
    override val fanPower: Int,
    override val waterBoxMode: Int,
    override val mopMode: Int,
    override val mopTemplateId: Int,
    override val repeat: Int,
    override val cleanOrderMode: Int,
    override val mapFlag: Int,
    override val tid: String
) : ExtendedCleanupParams

data class Zone(
    val repeat: Int,
    val zid: Int
)

data class ParamsForSegmentedCleanup(
    val segs: List<Segment>,
    override val fanPower: Int,
    override val waterBoxMode: Int,
    override val mopMode: Int,
    override val mopTemplateId: Int,
    override val repeat: Int,
    override val cleanOrderMode: Int,
    override val mapFlag: Int,
    override val tid: String
) : ExtendedCleanupParams

data class Segment(
    val sid: Int
)