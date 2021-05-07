package no.skatteetaten.aurora.fergus.service

import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.AuthorizeResponse.StatusEnum.SUCCESS
import org.openapitools.client.model.ErrorResponse
import org.openapitools.client.model.LocalizedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.server.ResponseStatusException

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

        assertThat(request.path).isEqualTo("/api/v3/authorize")
    }

    @Test
    fun `Should throw exception when authorize fails`() {
        val response = ErrorResponse().apply {
            status = ErrorResponse.StatusEnum.ERROR
            message = LocalizedError()
                .text("Your credentials for this account were invalid. Please try again.")
                .key("auth.401_login")
            code = 401
            apiVersion = "3.2"
        }
        val payload = AuthorizationPayload(
            accountId = "testAccount",
            username = "testUser",
            password = "testPass",
        )

        val requests = mockWebServer.executeBlocking(response) {
            assertThat { storageGridService.authorize(payload) }
                .isFailure().isInstanceOf(ResponseStatusException::class)
        }
        assertThat(requests).hasSize(1)
    }
}
