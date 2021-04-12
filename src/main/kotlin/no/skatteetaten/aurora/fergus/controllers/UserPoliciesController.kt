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
        @RequestBody @Valid provisionUserPoliciesPayload: ProvisionUserPoliciesPayload
    ): ProvisionUserPoliciesResponse {
        // TODO: Implement method - split subtasks into separate providers/services - remove comments when done
        // Call SG Man API to authenticate tenant root user for token (MVP)
        val authorizationPayload = AuthorizationPayload(
            provisionUserPoliciesPayload.tenantAccount.accountId,
            provisionUserPoliciesPayload.tenantAccount.username,
            provisionUserPoliciesPayload.tenantAccount.password
        )
        val token = storageGridService.authorize(authorizationPayload)

        // Verify Bucket existence and create Bucket if not (MVP)
        storageGridService.provideBucket(bucketname, token)
        // Call SG Man API to verify Group existence (MVP)
        // Call SG Man API to create Group with policy for the User for the Bucket and Path with the given access parameters (MVP)
        storageGridService.provideGroup(bucketname, path, provisionUserPoliciesPayload.access, token)
        // Call SG Man API to verify User existence (MVP)
        // Call SG Man API to create User and Group membership (MVP)
        // Call SG Man API to change User password (MVP)
        // Call SG Man API to create S3 Access Keys for named User (MVP)
        return ProvisionUserPoliciesResponse(provisionUserPoliciesPayload.username, provisionUserPoliciesPayload.password ?: "password", "host", "accesskey", "secretkey")
    }
}

data class ProvisionUserPoliciesPayload(
    val tenantAccount: TenantAccountInput,
    val username: String,
    val password: String?,
    val access: List<Access>,
)

data class TenantAccountInput(
    val accountId: String,
    val username: String,
    val password: String,
)

data class ProvisionUserPoliciesResponse(
    val username: String,
    val password: String,
    val host: String,
    val s3accesskey: String,
    val s3secretaccesskey: String,
)

enum class Access {
    READ, WRITE, DELETE
}
