package no.skatteetaten.aurora.openshift.reference.springboot.service

import java.lang.IllegalStateException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * An example service that demonstrates basic database operations.
 * Note the use of the kotlin extension functions for Spring JDBC.
 */
@Service
class CounterDatabaseService(private val jdbcTemplate: JdbcTemplate) {

    @Transactional
    fun getAndIncrementCounter(): Long {
        val counter: Long = jdbcTemplate.queryForObject("SELECT value FROM counter FOR UPDATE OF value")
            ?: throw IllegalStateException("counter table not initialized")
        jdbcTemplate.update("UPDATE counter SET value=value+1")
        return counter
    }

    val counter: Long get() = jdbcTemplate.queryForObject("SELECT value FROM counter") ?: 0L
}
