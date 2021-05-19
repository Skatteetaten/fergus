package no.skatteetaten.aurora.fergus.error

open class FergusException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
