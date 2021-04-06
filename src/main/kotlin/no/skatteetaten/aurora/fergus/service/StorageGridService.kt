package no.skatteetaten.aurora.fergus.service

import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.config.RequiresStorageGrid
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.openapitools.client.api.AuthApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.Credentials
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnBean(RequiresStorageGrid::class)
class StorageGridServiceReactive(private val storageGridAuthApi: AuthApi) : StorageGridService {
    override suspend fun authorize(
        authorizationPayload: AuthorizationPayload
    ): AuthorizeResponse = storageGridAuthApi
        .authorizePost(authorizationPayload.toAuthorizeInput())
        .awaitSingle()
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): AuthorizeResponse? = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

fun AuthorizationPayload.toAuthorizeInput(): Credentials = Credentials()
    .accountId(accountId)
    .username(username)
    .password(password)
