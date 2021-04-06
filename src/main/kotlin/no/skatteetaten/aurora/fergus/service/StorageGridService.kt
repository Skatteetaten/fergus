package no.skatteetaten.aurora.fergus.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.springframework.web.reactive.function.client.WebClient
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitSingleOrNull
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.RequiresStorageGrid
import no.skatteetaten.aurora.fergus.ServiceTypes
import no.skatteetaten.aurora.fergus.TargetService
import org.openapitools.client.api.AuthApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.Credentials

@Service
@ConditionalOnBean(RequiresStorageGrid::class)
class StorageGridServiceReactive(
    @TargetService(ServiceTypes.STORAGEGRID) private val webClient: WebClient,
    @TargetService(ServiceTypes.STORAGEGRID_AUTH) private val authClient: AuthApi,
    val objectMapper: ObjectMapper
) : StorageGridService {
    override suspend fun authorize(authorizationPayload: AuthorizationPayload): AuthorizeResponse? = webClient
        .get()
        .uri("/api/v3/authorize")
        .authorize()

    suspend fun authorizeWithAuthApi(
        authorizationPayload: AuthorizationPayload
    ): AuthorizeResponse? = authClient.authorizePost(
        Credentials()
            .accountId(authorizationPayload.accountId)
            .username(authorizationPayload.username)
            .password(authorizationPayload.password)
    ).awaitSingleOrNull()

    private suspend inline fun WebClient.RequestHeadersSpec<*>.authorize(): AuthorizeResponse? = null
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): AuthorizeResponse? = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

data class AuthorizeInput(
    val accountId: String,
    val username: String,
    val password: String,
)

fun AuthorizationPayload.toAuthorizeInput() = AuthorizeInput(
    accountId = accountId,
    username = username,
    password = password,
)
