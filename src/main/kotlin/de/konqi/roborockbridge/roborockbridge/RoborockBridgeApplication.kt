package de.konqi.roborockbridge.roborockbridge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RoborockBridgeApplication

fun main(args: Array<String>) {
    runApplication<RoborockBridgeApplication>(*args)
}
