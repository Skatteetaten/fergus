package no.skatteetaten.aurora.fergus.controllers

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
class UserPoliciesController() {

    @PostMapping("/buckets/{bucketname}/paths/{path}/userpolicies/")
    suspend fun provisionUserPolicies(
        @PathVariable bucketname: String,
        @PathVariable path: String,
        @RequestBody @Valid provisionUserPoliciesPayload: ProvisionUserPoliciesPayload
    ): ProvisionUserPoliciesResponse {
        // TODO: Implement method - split subtasks into separate providers/services
        // Call SG Man API to authenticate tenant root user for token (MVP)
        // Call SG Man API to create S3 Access Keys for tenant root user (MVP)
        // Call SG Man API to verify Bucket existence (MVP)
        // Call SG Man API to create Bucket (MVP)
        // Call SG Man API to verify User existence (MVP)
        // Call SG Man API to create User and set Password (MVP)
        // Call SG Man API to create S3 Access Keys for named User (MVP)
        // Call SG S3 API to write a canned Policy for the User for the Bucket and Path with the given access parameters (MVP)
        return ProvisionUserPoliciesResponse(bucketname, path, "host", "accesskey", "secretkey")
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
