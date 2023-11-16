package de.konqi.roborockbridge.roborockbridge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RoborockBridgeApplication

fun main(args: Array<String>) {
    runApplication<RoborockBridgeApplication>(*args)
}
