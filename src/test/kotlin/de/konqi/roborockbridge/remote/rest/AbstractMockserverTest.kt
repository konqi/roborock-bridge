package de.konqi.roborockbridge.remote.rest

import org.junit.ClassRule
import org.junit.jupiter.api.BeforeAll
import org.mockserver.client.MockServerClient
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractMockserverTest {
    companion object {
        private val MOCKSERVER_IMAGE: DockerImageName = DockerImageName
            .parse("mockserver/mockserver")
            .withTag("mockserver-" + MockServerClient::class.java.getPackage().implementationVersion)

        @JvmField
        @ClassRule
        val mockServer = MockServerContainer(MOCKSERVER_IMAGE)

        @JvmStatic
        @BeforeAll
        fun startMockserver(): Unit {
            mockServer.start()
        }
    }
}