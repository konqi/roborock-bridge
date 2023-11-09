package de.konqi.roborockbridge.roborockbridge.protocol

import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.Rriot
import khttp.options
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class RoborockMqtt(val rriot: Rriot) {
    private val username = Utils.calcHexMd5(arrayOf(rriot.userId, rriot.mqttKey).joinToString(":")).substring(2, 10)
    private val password = Utils.calcHexMd5(arrayOf(rriot.sessionId, rriot.mqttKey).joinToString(":")).substring(16)
    private val broker = rriot.remote.mqttServer
    // maybe store clientId somewhere or generate a static string e.g. MD5(username)
    private val clientId = "${Utils.CLIENT_ID_SHORT}_${Utils.generateNonce()}"

    private val persistence = MemoryPersistence()
    private val mqttClient = MqttClient(broker, clientId, persistence)


    fun connect() {
        val connectionOptions = MqttConnectOptions().apply {
            keepAliveInterval = 60
            isCleanSession = true
            userName = username
            password = password
        }

        mqttClient.connect(connectionOptions)


    }

    fun disconnect() {
        if(mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }
}