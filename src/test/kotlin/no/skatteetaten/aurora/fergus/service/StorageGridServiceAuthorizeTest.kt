package no.skatteetaten.aurora.fergus.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.AuthorizeResponse.StatusEnum.SUCCESS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@SpringBootTest(
    properties = [
        "integrations.storagegrid.url=http://localhost:9999",
        "aurora.token.value=testToken",
    ]
)
class StorageGridServiceAuthorizeTest {
    @Autowired
    lateinit var storageGridService: StorageGridService
    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start(9999)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authorizeDeepHappyTest() {
        val mockToken = "test token"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .setBody(
                    objectMapper.writeValueAsString(
                        AuthorizeResponse().apply {
                            status = SUCCESS
                            data = mockToken
                        }
                    )
                )
        )

        runBlocking {
            val body = AuthorizationPayload(
                accountId = "testAccount",
                username = "testUser",
                password = "testPass",
            )
            val token = storageGridService.authorize(body)
            val request = mockWebServer.takeRequest()

            assertThat(token).isEqualTo(mockToken)
            assertThat(request.getHeader(AUTHORIZATION)).isEqualTo("aurora-token testToken")
            assertThat(request.path).isEqualTo("/api/v3/authorize")
            assertThat(request.body.readUtf8()).isEqualTo(objectMapper.writeValueAsString(body.toAuthorizeInput()))
        }
    }
}
