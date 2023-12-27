package de.konqi.roborockbridge.remote.rest

import de.konqi.roborockbridge.TestBeanProvider
import de.konqi.roborockbridge.remote.RoborockCredentials
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.`when`
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [LoginApi::class, DefaultRestTemplate::class, TestBeanProvider::class])
class LoginApiTest(
    @Autowired val loginApi: LoginApi,
) : AbstractMockserverTest() {
    @MockBean
    lateinit var roborockCredentials: RoborockCredentials

    @BeforeEach
    fun beforeEach() {
        `when`(roborockCredentials.baseUrl).thenReturn("http://${mockServer.host}:${mockServer.serverPort}")
        `when`(roborockCredentials.username).thenReturn("maxmuster")
        `when`(roborockCredentials.password).thenReturn("retsumxam")
        `when`(roborockCredentials.clientId).thenReturn("clientId")
    }

    @Test
    fun testLogin() {
        val login = loginApi.login()

        assertThat(login.token).isEqualTo(TOKEN)

        assertAll(
            "rriot",
            { assertThat(login.rriot.sessionId).isEqualTo(SESSION_ID) },
            { assertThat(login.rriot.remote.api).isEqualTo(REMOTE_API) },
            { assertThat(login.rriot.remote.mqttServer).isEqualTo(MQTT_SERVER) },
            { assertThat(login.rriot.mqttKey).isEqualTo(MQTT_KEY) },
            { assertThat(login.rriot.userId).isEqualTo(USER_ID) },
            { assertThat(login.rriot.hmacKey).isEqualTo(HMAC_KEY) },
        )
    }

    companion object {
        const val TOKEN = "123456789abcdefghijk-toe+abcde/fghijkl=="
        const val HMAC_KEY = "0123456789"
        const val SESSION_ID = "012345"
        const val USER_ID = "0123456789012345678912"
        const val MQTT_SERVER = "ssl://mqtt-eu-5.roborock.com:8883"
        const val REMOTE_API = "https://api-eu.roborock.com"
        const val MQTT_KEY = "01234567"

        val mockserverClient = MockServerClient(mockServer.host, mockServer.serverPort)

        @JvmStatic
        @BeforeAll
        fun addMocks() {
            mockserverClient.reset().`when`(
                request()
                    .withMethod("POST")
                    .withPath("/api/v1/login")
                    .withQueryStringParameter(Parameter.param("username", ".+"))
                    .withQueryStringParameter(Parameter.param("password", ".+"))
                    .withQueryStringParameter("needtwostepauth", "false")
                    .withHeader(Header.header("header_clientid", ".+")), Times.exactly(1)
            ).respond(
                response().withStatusCode(200).withBody(
                    """
                    {
                        "code" : 200,
                        "msg" : "success",
                        "data" : {
                          "uid" : 1234567,
                          "tokentype" : "",
                          "token" : "$TOKEN",
                          "rruid" : "rr1234567890",
                          "region" : "eu",
                          "countrycode" : "49",
                          "country" : "DE",
                          "nickname" : "Max Mustermann",
                          "rriot" : {
                            "u" : "$USER_ID",
                            "s" : "$SESSION_ID",
                            "h" : "$HMAC_KEY",
                            "k" : "$MQTT_KEY",
                            "r" : {
                              "r" : "EU",
                              "a" : "$REMOTE_API",
                              "m" : "$MQTT_SERVER",
                              "l" : "https://wood-eu.roborock.com"
                            }
                          },
                          "tuyaDeviceState" : 0,
                          "avatarurl" : "https://files.roborock.com/iottest/default_avatar.png"
                        }
                    }""".trimIndent(), MediaType.APPLICATION_JSON
                )
            )
        }
    }
}