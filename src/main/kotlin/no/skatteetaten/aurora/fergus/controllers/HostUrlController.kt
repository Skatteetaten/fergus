package no.skatteetaten.aurora.fergus.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping

/*
 * A REST controller defining endpoint for getting s3 host
 *
 * This is endpoint is mostly for test purposes and may be deleted when not needed any more
 */
@RestController
@RequestMapping("/v1")
class HostUrlController(
    @Value("\${integrations.storagegrid.s3url}") val s3Url: String,
) {
    @GetMapping("/s3Url")
    suspend fun s3Url(): String = s3Url
}
