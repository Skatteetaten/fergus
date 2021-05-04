package no.skatteetaten.aurora.fergus.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping

/*
 * A REST controller defining endpoints for getting s3 host
 */
@RestController
@RequestMapping("/v1")
class HostUrlController(
    @Value("\${integrations.storagegrid.s3url}") val s3Url: String,
) {
    @GetMapping("/s3Url")
    suspend fun s3Url(): String = s3Url
}
