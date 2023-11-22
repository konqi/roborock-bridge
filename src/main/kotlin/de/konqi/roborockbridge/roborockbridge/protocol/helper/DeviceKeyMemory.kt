package de.konqi.roborockbridge.roborockbridge.protocol.helper

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component


@ConfigurationProperties(prefix = "override")
data class DeviceKeyMemoryOverride(var deviceMemory: Map<String, String> = emptyMap())

@Component
@EnableConfigurationProperties(DeviceKeyMemoryOverride::class)
class DeviceKeyMemory(
    @Autowired
    val config: DeviceKeyMemoryOverride?
) : HashMap<String, String>(10), InitializingBean {
    override fun afterPropertiesSet() {
        config?.deviceMemory?.forEach { (key, value) ->
            put(key, value)
        }
    }
}