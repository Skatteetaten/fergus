package no.skatteetaten.aurora.fergus.service

import org.springframework.stereotype.Service
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload

@Service
class StorageGridService {

    fun authorize(authorizationPayload: AuthorizationPayload): String? {

        return null
    }
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

data class AuthorizeResponse(
    val status: String,
    val apiVersion: String,
    val deprecated: Boolean?,
    val data: String,
)
