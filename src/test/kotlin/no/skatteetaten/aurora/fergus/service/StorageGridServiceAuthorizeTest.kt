package no.skatteetaten.aurora.fergus.service

import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
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

@SpringBootTest(
    properties = [
        "integrations.storagegrid.url=http://localhost:9999",
        "aurora.token.value=testToken",
    ]
)
class StorageGridServiceAuthorizeTest {
    @Autowired
    lateinit var storageGridService: StorageGridService

    private var mockWebServer = MockWebServer()

    @BeforeEach
    fun setup() {
        mockWebServer.start(9999)
    }

    @AfterEach
    fun tearDown() {
        runCatching { mockWebServer.shutdown() }
    }

    @Test
    fun authorizeDeepHappyTest() {
        val mockToken = "test token"
        val response = AuthorizeResponse().apply {
            status = SUCCESS
            data = mockToken
        }

        val request = mockWebServer.executeBlocking(response) {
            val body = AuthorizationPayload(
                accountId = "testAccount",
                username = "testUser",
                password = "testPass",
            )
            val token = storageGridService.authorize(body)

            assertThat(token).isEqualTo(mockToken)
        }.first()!!

        assertThat(request.getHeader(AUTHORIZATION)).isEqualTo("aurora-token testToken")
        assertThat(request.path).isEqualTo("/api/v3/authorize")
    }
}
