package no.skatteetaten.aurora.fergus.service

import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.openapitools.client.api.AuthApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.Credentials
import org.springframework.stereotype.Service

@Service
class StorageGridServiceReactive(private val storageGridAuthApi: AuthApi) : StorageGridService {
    override suspend fun authorize(
        authorizationPayload: AuthorizationPayload
    ): String {
        val response: AuthorizeResponse = storageGridAuthApi
            .authorizePost(authorizationPayload.toAuthorizeInput())
            .awaitSingle()
        if (response.status === AuthorizeResponse.StatusEnum.ERROR) {
            throw FergusException("The Storagegrid auth api returned an error")
        }
        return response.data // Returns authorization token
    }
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): String = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

fun AuthorizationPayload.toAuthorizeInput(): Credentials = Credentials()
    .accountId(accountId)
    .username(username)
    .password(password)
