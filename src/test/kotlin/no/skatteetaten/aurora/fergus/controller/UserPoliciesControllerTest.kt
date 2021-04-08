package no.skatteetaten.aurora.fergus.controller

import no.skatteetaten.aurora.fergus.controllers.UserPoliciesController
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserPoliciesController::class)
class UserPoliciesControllerTest : AbstractTestController() {

    @Test
    fun happyTest() {
        val input =
            """{"tenantAccount":{"accountId":"accountId","username":"tausername","password":"tapassword"}, "username":"username", "password":"password", "access":["READ"]}"""
        mvc.perform(
            post("/v1/buckets/bucket-1/paths/path-test/userpolicies/")
                .contentType("application/json")
                .content(input)
        ).andExpect(status().isOk)
    }
}
