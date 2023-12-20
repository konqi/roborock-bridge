package de.konqi.roborockbridge

import org.h2.tools.Server
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.sql.SQLException


@SpringBootApplication
@EnableScheduling
@EnableAsync
class RoborockBridgeApplication {
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(
        SQLException::class
    )
    @Profile("dev")
    fun inMemoryH2DatabaseServer(): Server {
        return Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9090"
        )
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(RoborockBridgeApplication::class.java)
    app.setAdditionalProfiles("bridge")
    app.setBannerMode(Banner.Mode.OFF)
    app.run(*args)
}
