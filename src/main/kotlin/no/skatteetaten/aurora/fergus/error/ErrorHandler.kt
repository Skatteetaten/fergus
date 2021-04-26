package no.skatteetaten.aurora.fergus.error

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just

private val logger = KotlinLogging.logger {}

data class GenericErrorResponse(
    val errorMessage: String,
    val cause: String? = null
)

@Suppress("unused")
@Component
@Order(-2)
class ErrorHandler : WebExceptionHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> = when (ex) {
        is ResponseStatusException -> handleResponseException(ex, exchange)
        is IllegalArgumentException -> handleException(ex, exchange, status = BAD_REQUEST)
        is AccessDeniedException -> handleException(ex, exchange, status = UNAUTHORIZED)
        else -> handleException(ex, exchange)
    }

    private fun handleResponseException(
        ex: ResponseStatusException,
        exchange: ServerWebExchange
    ): Mono<Void> {
        exchange.response.headers.putAll(ex.responseHeaders)
        exchange.response.statusCode = ex.status
        val buffer = exchange.response.bufferFactory().wrap(ex.reason?.toByteArray() ?: "".toByteArray())

        return exchange.response.writeWith(just(buffer))
    }

    @Suppress("SameParameterValue")
    private fun handleException(
        e: Throwable,
        exchange: ServerWebExchange,
        error: String = jacksonObjectMapper().writeValueAsString(
            GenericErrorResponse(
                e.message ?: "Unknown Error",
                e.cause?.message
            )
        ),
        status: HttpStatus = INTERNAL_SERVER_ERROR
    ): Mono<Void> {
        logger.error(e) { "Error in request" }

        exchange.response.headers.putAll(standardHeaders())
        exchange.response.statusCode = status
        val buffer = exchange.response.bufferFactory().wrap(error.toByteArray())

        return exchange.response.writeWith(just(buffer))
    }

    private fun standardHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = APPLICATION_JSON
    }
}
