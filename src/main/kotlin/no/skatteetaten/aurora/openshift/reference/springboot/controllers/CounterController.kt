package no.skatteetaten.aurora.openshift.reference.springboot.controllers

import no.skatteetaten.aurora.openshift.reference.springboot.service.CounterDatabaseService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * This is an example Controller that demonstrates a very simple controller that increments a counter
 * in a oracle sql database and returns the previous value.
 *
 * There should automatically be registered metrics for both the withMetrics block and the http endpoint
 * execute and http_server_request histograms
 */

data class CounterResponse(val value: Long)

@RestController
class CounterController(private val service: CounterDatabaseService) {

    @GetMapping("/api/counter")
    fun getCounter() = CounterResponse(service.getAndIncrementCounter())
}
