package no.skatteetaten.aurora.fergus

open class FergusException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
