package no.skatteetaten.aurora.fergus.service

import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.ContainersApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.ContainerListResponse
import org.openapitools.client.model.Credentials
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class StorageGridServiceReactive(
    private val storageGridAuthApi: AuthApi,
    private val storageGridContainersApi: ContainersApi,
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
        val bucketListResponse: ContainerListResponse = storageGridContainersApi
            .orgContainersGet(listOf<String>())
            .awaitSingle()
        if (bucketListResponse.status === ContainerListResponse.StatusEnum.ERROR){
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid containers api returned an error"
            )
        }
        // TODO: Check if bucketName exists in bucketListResponse, if not, create

        return response.data // Returns bucket name
    }
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): String = integrationDisabled()

    suspend fun provideBucket(bucketName: String): String = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

fun AuthorizationPayload.toAuthorizeInput(): Credentials = Credentials()
    .accountId(accountId)
    .username(username)
    .password(password)
