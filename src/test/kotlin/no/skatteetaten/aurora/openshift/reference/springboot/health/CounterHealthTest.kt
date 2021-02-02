package no.skatteetaten.aurora.openshift.reference.springboot.health

import no.skatteetaten.aurora.openshift.reference.springboot.service.CounterDatabaseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class CounterHealthTest {

    @MockBean
    private lateinit var counterDatabaseService: CounterDatabaseService

    @Test
    fun `Verifies status varies on counter value`() {

        val counterHealth = CounterHealth(counterDatabaseService)

        given(counterDatabaseService.counter).willReturn(2)
        assertThat(counterHealth.health().status.code).isEqualTo("OBSERVE")

        given(counterDatabaseService.counter).willReturn(3)
        assertThat(counterHealth.health().status.code).isEqualTo("UP")
    }
}
