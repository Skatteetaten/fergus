package no.skatteetaten.aurora.fergus.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.fergus.controllers.UserPoliciesController
import no.skatteetaten.aurora.fergus.service.S3AccessKeys
import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(UserPoliciesController::class)
class UserPoliciesControllerTest : AbstractTestController() {

    @MockkBean
    private lateinit var storageGridService: StorageGridService

    @Test
    fun bucketsPathsUserpoliciesHappyTest() {
        coEvery {
            storageGridService.authorize(any())
        } returns "testtoken"
        coEvery {
            storageGridService.provideBucket("bucket-1", "testtoken")
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

        val input =
            """{"tenantAccount":{"accountId":"accountId","username":"tausername","password":"tapassword"}, "username":"username", "password":"passord", "access":["READ"]}"""
        val mvcResult: MvcResult = mvc.perform(
            post("/v1/buckets/bucket-1/paths/path-test/userpolicies/")
                .contentType("application/json")
                .content(input)
        ).andExpect(request().asyncStarted()).andReturn()

        mvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"username\":\"username\",\"password\":\"passord\",\"host\":\"host\",\"s3accesskey\":\"s3AccessKey\",\"s3secretaccesskey\":\"s3SecretKey\"}"))
    }
}
