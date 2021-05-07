package no.skatteetaten.aurora.fergus.controller

import assertk.assertThat
import assertk.assertions.contains
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.fergus.controllers.AuthorizationController
import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters

@WebFluxTest(AuthorizationController::class)
@Import(WebClientAutoConfiguration::class)
class AuthorizationControllerTest {

    @MockkBean
    private lateinit var storageGridService: StorageGridService
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun authorizeHappyTest() {
        coEvery {
            storageGridService.authorize(any())
        } returns "testtoken"

        val resultstring = webTestClient
            .post()
            .uri("/v1/authorize")
            .contentType(MediaType.APPLICATION_JSON)
            .body(createBody())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .returnResult().toString()
        assertThat(resultstring).contains("testtoken")
    }

    private fun createBody(): BodyInserter<String, ReactiveHttpOutputMessage> = BodyInserters.fromValue(
        """{"accountId":"accountId","username":"tausername","password":"tapassword"}"""
    )
}
