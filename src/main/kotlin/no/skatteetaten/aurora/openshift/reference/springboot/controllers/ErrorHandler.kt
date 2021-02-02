package no.skatteetaten.aurora.openshift.reference.springboot.controllers

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * A sample error handler. You can add your own exceptions below to control the error codes that should be used in
 * various error scenarios.
 */

data class ErrorResponse(val errorMessage: String, val cause: String? = null)

@ControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(RuntimeException::class)
    protected fun handleGenericError(e: RuntimeException, request: WebRequest): ResponseEntity<Any> =
        handleException(e, request, HttpStatus.INTERNAL_SERVER_ERROR)

    @ExceptionHandler(IllegalArgumentException::class)
    protected fun handleBadRequest(e: IllegalArgumentException, request: WebRequest): ResponseEntity<Any> =
        handleException(e, request, HttpStatus.BAD_REQUEST)

    private fun handleException(e: RuntimeException, request: WebRequest, httpStatus: HttpStatus): ResponseEntity<Any> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val errorResponse = ErrorResponse(e.message ?: "Unknown error", e.cause?.message)
        return handleExceptionInternal(e, errorResponse, headers, httpStatus, request)
    }
}
