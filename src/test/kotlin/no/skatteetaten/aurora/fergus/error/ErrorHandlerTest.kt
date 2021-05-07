package no.skatteetaten.aurora.fergus.error

import assertk.assertThat
import assertk.assertions.contains
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.fergus.controllers.AuthorizationController
import no.skatteetaten.aurora.fergus.controllers.UserPoliciesController
import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.server.ResponseStatusException

@WebFluxTest(UserPoliciesController::class, AuthorizationController::class)
@Import(WebClientAutoConfiguration::class)
class ErrorHandlerTest {
    @MockkBean
    private lateinit var storageGridService: StorageGridService
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `Testing that ErrorHandler catches ResponseStatusException`() {
        val responseStatusException = ResponseStatusException(HttpStatus.CONFLICT, "The reason it occurred")
        coEvery {
            storageGridService.authorize(any())
        } throws (responseStatusException)

        webTestClient
            .post()
            .uri("/v1/buckets/bucket-1/paths/path-test/userpolicies/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(createUserPoliciesBody())
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody()
            .returnResult().toString().contains("The reason it occurred")
    }

    private fun createUserPoliciesBody(): BodyInserter<String, ReactiveHttpOutputMessage> = BodyInserters.fromValue(
        """{"tenantAccount":{"accountId":"accountId","username":"tausername","password":"tapassword"}, "username":"username", "password":"passord", "access":["READ"]}"""
    )

    @Test
    fun `Testing that ErrorHandler catches generic exception`() {
        val exception = Exception("The reason it occurred")
        coEvery {
            storageGridService.authorize(any())
        } throws (exception)

        val resultstring = webTestClient
            .post()
            .uri("/v1/authorize")
            .contentType(MediaType.APPLICATION_JSON)
            .body(createAuthorizeBody())
            .exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .returnResult().toString()
        assertThat(resultstring).contains("The reason it occurred")
    }

    private fun createAuthorizeBody(): BodyInserter<String, ReactiveHttpOutputMessage> = BodyInserters.fromValue(
        """{"accountId":"accountId","username":"tausername","password":"tapassword"}"""
    )
}
