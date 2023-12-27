package de.konqi.roborockbridge.remote.rest

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.RoborockCredentials
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [HomeApi::class, HomeApiRestTemplate::class, TestBeanProvider::class])
class HomeApiTest(@Autowired val homeApi: HomeApi) : AbstractMockserverTest() {
    @MockBean
    lateinit var roborockCredentials: RoborockCredentials

    @Test
    fun getHome() {
        `when`(roborockCredentials.baseUrl).thenReturn("http://${mockServer.host}:${mockServer.serverPort}")
        `when`(roborockCredentials.restApiToken).thenReturn(REST_API_TOKEN)

        mockserverClient.`when`(
            request()
                .withMethod("GET")
                .withPath("/api/v1/getHomeDetail")
                .withHeaders(header("Authorization", REST_API_TOKEN)),
            Times.exactly(1)
        ).respond(
            HttpResponse.response().withStatusCode(200).withBody(
                """{
                      "code": 200,
                      "msg": "success",
                      "data": {
                        "id": 2345678,
                        "name": "My Home",
                        "tuyaHomeId": 0,
                        "rrHomeId": $MOCKED_HOME_ID,
                        "deviceListOrder": null
                      }
                   }""".trimIndent(), MediaType.APPLICATION_JSON
            )
        )

        val home = homeApi.getHome()

        assertThat(home.rrHomeId).isEqualTo(MOCKED_HOME_ID)
    }

    companion object {
        const val REST_API_TOKEN = "this is the restApiToken from auth.json"
        const val MOCKED_HOME_ID = 1234567

        val mockserverClient = MockServerClient(mockServer.host, mockServer.serverPort)

        @JvmStatic
        @BeforeAll
        fun addMocks() {
            mockserverClient.reset()
        }
    }
}