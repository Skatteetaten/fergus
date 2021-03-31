package no.skatteetaten.aurora.fergus.controllers

import javax.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    suspend fun authorize(@RequestBody @Valid authorizationPayload: AuthorizationPayload): ResponseEntity<String?> {
        val authorizeResponse = storageGridService.authorize(authorizationPayload)
        return if (authorizeResponse != null) {
            ResponseEntity.ok(authorizeResponse.data)
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
}

data class AuthorizationPayload(
    val username: String,
    val password: String,
)
