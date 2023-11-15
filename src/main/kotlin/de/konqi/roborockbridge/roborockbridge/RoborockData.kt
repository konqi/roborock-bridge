package de.konqi.roborockbridge.roborockbridge

import de.konqi.roborockbridge.roborockbridge.protocol.dto.login.Rriot
import de.konqi.roborockbridge.roborockbridge.state.Home
import de.konqi.roborockbridge.roborockbridge.state.Robot
import org.springframework.context.annotation.Configuration

@Configuration
class RoborockData {
    lateinit var home: Home
    lateinit var robots: List<Robot>
    lateinit var rriot: Rriot
}