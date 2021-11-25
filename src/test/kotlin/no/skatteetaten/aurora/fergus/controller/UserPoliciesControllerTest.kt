package no.skatteetaten.aurora.fergus.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.fergus.controllers.UserPoliciesController
import no.skatteetaten.aurora.fergus.service.S3AccessKeys
import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import java.util.UUID

@WebFluxTest(UserPoliciesController::class)
@Import(WebClientAutoConfiguration::class)
class UserPoliciesControllerTest {

    @MockkBean
    private lateinit var storageGridService: StorageGridService
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun bucketsPathsUserpoliciesHappyTest() {
        coEvery {
            storageGridService.authorize(any())
        } returns "testtoken"
        coEvery {
            storageGridService.provideBucket("bucket-1", "no-skatt-1", "testtoken")
        } returns "bucket-1"
        coEvery {
            storageGridService.provideGroup("bucket-1", "path-test", any(), "testtoken")
        } returns UUID.fromString("847b3329-9855-4da9-bf3b-b2162e993d5d")
        coEvery {
            storageGridService.provideUser("username", UUID.fromString("847b3329-9855-4da9-bf3b-b2162e993d5d"), "testtoken")
        } returns UUID.fromString("7b579332-87c5-4dc2-9ba0-dfe9dda435c3")
        coEvery {
            storageGridService.assignPasswordToUser(UUID.fromString("7b579332-87c5-4dc2-9ba0-dfe9dda435c3"), "passord", "testtoken")
        } returns "passord"
        coEvery {
            storageGridService.provideS3AccessKeys(UUID.fromString("7b579332-87c5-4dc2-9ba0-dfe9dda435c3"), "testtoken")
        } returns S3AccessKeys("s3AccessKey", "s3SecretKey")

        webTestClient
            .post()
            .uri("/v1/buckets/bucket-1/paths/path-test/userpolicies")
            .contentType(MediaType.APPLICATION_JSON)
            .body(createBody())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .json("{\"username\":\"username\",\"password\":\"passord\",\"host\":\"http://uia0ins-netapp-storagegrid01.skead.no:10880/\",\"s3accesskey\":\"s3AccessKey\",\"s3secretaccesskey\":\"s3SecretKey\"}")
    }

    private fun createBody(): BodyInserter<String, ReactiveHttpOutputMessage> = BodyInserters.fromValue(
        """{"tenantAccount":{"accountId":"accountId","username":"tausername","password":"tapassword"}, "username":"username", "password":"passord", "access":["READ"]}"""
    )
}
