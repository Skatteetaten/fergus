package no.skatteetaten.aurora.fergus.controllers

import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.springframework.beans.factory.annotation.Value
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
class UserPoliciesController(
    private val storageGridService: StorageGridService,
    @Value("\${integrations.storagegrid.s3url}") val s3Url: String,
) {
    @PostMapping("/buckets/{bucketname}/paths/{path}/userpolicies")
    suspend fun provisionUserPolicies(
        @PathVariable bucketname: String,
        @PathVariable path: String,
        @RequestBody @Valid provisionUserPoliciesPayload: ProvisionUserPoliciesPayload
    ): ProvisionUserPoliciesResponse {
        // Call SG Man API to authenticate tenant root user for token (MVP)
        val authorizationPayload = AuthorizationPayload(
            provisionUserPoliciesPayload.tenantAccount.accountId,
            provisionUserPoliciesPayload.tenantAccount.username,
            provisionUserPoliciesPayload.tenantAccount.password
        )
        val token = storageGridService.authorize(authorizationPayload)
        // Verify Bucket existence and create Bucket if not (MVP)
        storageGridService.provideBucket(bucketname, token)
        // Verify Group existence and create Group with Policy if not (MVP)
        val groupId = storageGridService.provideGroup(bucketname, path, provisionUserPoliciesPayload.access, token)
        // Verify User existence and create User if not. Assign/Update Group membership (MVP)
        val userId = storageGridService.provideUser(provisionUserPoliciesPayload.username, groupId, token)
        // Assign specified password to User or generate if null (MVP)
        val password = storageGridService.assignPasswordToUser(userId, provisionUserPoliciesPayload.password, token)
        // Create S3 Access Keys for User (MVP)
        val s3keys = storageGridService.provideS3AccessKeys(userId, token)

        return ProvisionUserPoliciesResponse(
            provisionUserPoliciesPayload.username,
            password,
            s3Url,
            s3keys.s3accesskey,
            s3keys.s3secretaccesskey
        )
    }
}

data class ProvisionUserPoliciesPayload(
    val tenantAccount: TenantAccountInput,
    val username: String,
    val password: String? = null,
    val access: List<Access> = emptyList(),
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
