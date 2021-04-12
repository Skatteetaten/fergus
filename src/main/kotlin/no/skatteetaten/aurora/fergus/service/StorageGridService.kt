package no.skatteetaten.aurora.fergus.service

import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.controllers.Access
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.ContainersApi
import org.openapitools.client.api.GroupsApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.ContainerCreate
import org.openapitools.client.model.ContainerCreateResponse
import org.openapitools.client.model.ContainerListResponse
import org.openapitools.client.model.Credentials
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class StorageGridServiceReactive(
    private val storageGridAuthApi: AuthApi,
    private val storageGridContainersApi: ContainersApi,
    private val storageGridGroupsApi: GroupsApi,
) : StorageGridService {
    override suspend fun authorize(
        authorizationPayload: AuthorizationPayload
    ): String {
        val response: AuthorizeResponse = storageGridAuthApi
            .authorizePost(authorizationPayload.toAuthorizeInput())
            .awaitSingle()
        if (response.status === AuthorizeResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid auth api returned an error"
            )
        }
        return response.data // Returns authorization token
    }

    override suspend fun provideBucket(
        bucketName: String,
        token: String
    ): String {
        storageGridContainersApi.apiClient.setBearerToken(token)
        // Get list of buckets for tenant
        val bucketListResponse = storageGridContainersApi
            .orgContainersGet(listOf<String>())
            .awaitSingle()
        if (bucketListResponse.status === ContainerListResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid containers api returned an error on orgContainersGet"
            )
        }
        // Check if bucketName exists in bucketListResponse, if not, create
        val bucketNames: List<String> = bucketListResponse.data.map { it.name }
        if (!bucketNames.contains(bucketName)) {
            val containerCreate = ContainerCreate().name(bucketName)
            val containerCreateResponse = storageGridContainersApi
                .orgContainersPost(containerCreate)
                .awaitSingle()
            if (containerCreateResponse.status === ContainerCreateResponse.StatusEnum.ERROR) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Storagegrid containers api returned an error on orgContainersPost"
                )
            }
        }

        return bucketName
    }

    override suspend fun provideGroup(bucketname: String, path: String, access: List<Access>, token: String): String {
        storageGridGroupsApi.apiClient.setBearerToken(token)

        val groupName: String

        // TODO: FROM fiona
        policyNamePostfix := ""
        for _, s := range createAppUserInput.Access {
            policyNamePostfix += s[0:1]
        }
        policyName := fmt.Sprintf("%s_%s_%s", bucket, path, , policyNamePostfix)


        // Get list of buckets for tenant
        val bucketListResponse = storageGridGroupsApi
            .orgContainersGet(listOf<String>())
            .awaitSingle()
        if (bucketListResponse.status === ContainerListResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid containers api returned an error on orgContainersGet"
            )
        }
        // Check if bucketName exists in bucketListResponse, if not, create
        val bucketNames: List<String> = bucketListResponse.data.map { it.name }
        if (!bucketNames.contains(bucketName)) {
            val containerCreate = ContainerCreate().name(bucketName)
            val containerCreateResponse = storageGridGroupsApi
                .orgContainersPost(containerCreate)
                .awaitSingle()
            if (containerCreateResponse.status === ContainerCreateResponse.StatusEnum.ERROR) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Storagegrid containers api returned an error on orgContainersPost"
                )
            }
        }

        return bucketName
    }

}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): String = integrationDisabled()

    suspend fun provideBucket(bucketName: String, token: String): String = integrationDisabled()

    suspend fun provideGroup(bucketname: String, path: String, access: List<Access>, token: String): String = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

fun AuthorizationPayload.toAuthorizeInput(): Credentials = Credentials()
    .accountId(accountId)
    .username(username)
    .password(password)
