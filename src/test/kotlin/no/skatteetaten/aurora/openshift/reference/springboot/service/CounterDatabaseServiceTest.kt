package no.skatteetaten.aurora.openshift.reference.springboot.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@JdbcTest
@Import(CounterDatabaseService::class)
class CounterDatabaseServiceTest {

    @Autowired
    lateinit var service: CounterDatabaseService

    @Test
    fun `Verify maintains counter`() {

        assertThat(service.getAndIncrementCounter()).isEqualTo(0L)

        assertThat(service.getAndIncrementCounter()).isEqualTo(1L)
        assertThat(service.counter).isEqualTo(2L)
    }
}
