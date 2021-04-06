package no.skatteetaten.aurora.fergus

import no.skatteetaten.aurora.fergus.service.StorageGridService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "integrations.storagegrid.url=http://localhost:8080",
        "aurora.token.value=testToken",
    ]
)
class ApplicationTest {
    @Autowired
    lateinit var storageGridService: StorageGridService

    @Test
    fun servicesRunning() {
        assertThat(storageGridService).isNotNull
    }
}
