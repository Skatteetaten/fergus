package no.skatteetaten.aurora.fergus.controllers

import javax.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/*
 * A REST controller defining endpoints for buckets
 */
@RestController
@RequestMapping("/v1")
class BucketController() {

    @PostMapping("/bucket/")
    fun bucketCreate(@RequestBody @Valid bucketPayload: BucketCreatePayload) {
    }
}

data class BucketCreatePayload(
    val name: String,
)
