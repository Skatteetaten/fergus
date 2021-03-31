package no.skatteetaten.aurora.fergus.service

import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.springframework.web.reactive.function.client.WebClient
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.ServiceTypes
import no.skatteetaten.aurora.fergus.TargetService

class StorageGridServiceReactive(
    @TargetService(ServiceTypes.STORAGEGRID) private val webClient: WebClient
) : StorageGridService {
    override suspend fun authorize(authorizationPayload: AuthorizationPayload): AuthorizeResponse? = webClient
        .post()
        .uri("/api/v3/authorize")
        .bodyValue(authorizationPayload)
        .retrieve()
        .bodyToMono(AuthorizeResponse::class.java)
        .block()
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): AuthorizeResponse? = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

data class AuthorizeInput(
    val username: String,
    val password: String,
)

fun AuthorizationPayload.toAuthorizeInput() = AuthorizeInput(
    username = username,
    password = password,
)

data class AuthorizeResponse(
    val status: String,
    val apiVersion: String,
    val deprecated: Boolean?,
    val data: String,
)
