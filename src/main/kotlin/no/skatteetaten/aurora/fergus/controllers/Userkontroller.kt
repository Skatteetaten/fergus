package no.skatteetaten.aurora.fergus.controllers

import javax.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/*
 * A REST controller defining endpoints for users
 */
@RestController
@RequestMapping("/v1")
class UserController() {

    @PostMapping("/user/")
    fun userCreate(@RequestBody @Valid userPayload: UserCreatePayload) {
    }
}

data class UserCreatePayload(
    val name: String,
)
