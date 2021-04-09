package no.skatteetaten.aurora.fergus.controllers

import javax.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import no.skatteetaten.aurora.fergus.service.StorageGridService

/*
 * A REST controller defining endpoints for buckets
 */
@RestController
@RequestMapping("/v1")
class AuthorizationController(private val storageGridService: StorageGridService) {

    @PostMapping("/authorize")
    suspend fun authorize(@RequestBody @Valid authorizationPayload: AuthorizationPayload): String =
        storageGridService.authorize(authorizationPayload)
}

data class AuthorizationPayload(
    val accountId: String,
    val username: String,
    val password: String,
)
