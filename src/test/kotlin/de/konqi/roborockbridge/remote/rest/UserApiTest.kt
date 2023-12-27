package de.konqi.roborockbridge.remote.rest

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.RoborockCredentials
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpTemplate
import org.mockserver.model.HttpTemplate.template
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [UserApi::class, UserApiRestTemplate::class, TestBeanProvider::class])
class UserApiTest(@Autowired val userApi: UserApi) : AbstractMockserverTest() {
    @MockBean
    lateinit var roborockCredentials: RoborockCredentials

    @BeforeEach
    fun beforeEach() {
        `when`(roborockCredentials.restApiRemote).thenReturn("http://${mockServer.host}:${mockServer.serverPort}")
        `when`(roborockCredentials.hmacKey).thenReturn("Abcde12345")
        `when`(roborockCredentials.userId).thenReturn("12345Abcde67890FghiJ12")
        `when`(roborockCredentials.sessionId).thenReturn("AbCdEf")
    }

    @Test
    fun testGetUserHome() {
        val getHomesResponseTemplate =
            UserApiTest::class.java.getResource("GetHomesResultTemplate.json.hbs")
                ?.readText()
        mockserverClient.`when`(
            request()
                .withMethod("GET")
                .withPath("/user/homes/[0-9]+")
                .withHeader(rrAuthHeader),
            Times.exactly(1)
        ).respond(template(HttpTemplate.TemplateType.MUSTACHE, getHomesResponseTemplate))

        val result = userApi.getUserHome(HOME_ID)

        // Minimum assumption: There must be at least on device and product
        assertThat(result.devices).size().isPositive
        assertThat(result.products).size().isPositive
    }

    @Test
    fun testGetCleanupSchemas() {
        val getScenesResponseTemplate =
            UserApiTest::class.java.getResource("GetScenesResultTemplate.json.hbs")
                ?.readText()

        mockserverClient.`when`(
            request()
                .withMethod("GET")
                .withPath("/user/scene/home/[0-9]+")
                .withHeader(rrAuthHeader),
            Times.exactly(1)
        ).respond(template(HttpTemplate.TemplateType.MUSTACHE, getScenesResponseTemplate))


        val schemas = userApi.getCleanupScenes(HOME_ID)
        // Minimum assumption: There is one or more scene defined
        assertThat(schemas).size().isPositive
    }

    @Test
    fun testStartCleanup() {
        mockserverClient.`when`(
            request()
                .withMethod("POST")
                .withPath("/user/scene/[0-9]*/execute")
                .withHeader(rrAuthHeader),
            Times.exactly(1)
        ).respond(
            HttpResponse.response().withStatusCode(200).withBody(
                """
                    {
                      "api": null,
                      "result": null,
                      "status": "ok",
                      "success": true
                    }
                """.trimIndent(), MediaType.APPLICATION_JSON
            )
        )

        val result = userApi.startCleanupSchema(SCENE_ID)
        assertThat(result).isTrue()
    }

    companion object {
        const val HOME_ID = 1234567
        const val SCENE_ID = 123456

        //        val forwardToRR = forwardOverriddenRequest(
//            request().withHeaders(header("host", "api-eu.roborock.com")).withSocketAddress(
//                "api-eu.roborock.com", 443, SocketAddress.Scheme.HTTPS
//            )
//        )
        val rrAuthHeader: Header = header("Authorization", "Hawk .+")

        val mockserverClient = MockServerClient(mockServer.host, mockServer.serverPort)

        @JvmStatic
        @BeforeAll
        fun addMocks() {
            mockserverClient.reset()
        }
    }
}