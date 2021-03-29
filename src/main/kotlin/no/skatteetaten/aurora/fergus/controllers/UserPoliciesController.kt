package no.skatteetaten.aurora.fergus.controllers

import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.validation.Valid

/*
 * A REST controller defining endpoints for provisioning user policies
 */
@RestController
@RequestMapping("/v1")
class UserPoliciesController(private val storageGridService: StorageGridService) {

    @PostMapping("/buckets/{bucketname}/paths/{path}/userpolicies/")
    suspend fun provisionUserPolicies(
        @PathVariable bucketname: String,
        @PathVariable path: String,
        @RequestBody @Valid userPayload: UserCreatePayload
    ): UserPoliciesResponse {
        // TODO: Implement method
        return UserPoliciesResponse(bucketname, path, "host", "accesskey", "secretkey")
    }
}

data class UserCreatePayload(
    val tenantAccount: TenantAccount,
    val username: String,
    val password: String,
    val access: List<Access>,
)

data class TenantAccount(
    val accountId: String,
    val username: String,
    val password: String,
)

data class UserPoliciesResponse(
    val username: String,
    val password: String,
    val host: String,
    val s3accesskey: String,
    val s3secretaccesskey: String,
)

enum class Access {
    READ, WRITE, DELETE
}
