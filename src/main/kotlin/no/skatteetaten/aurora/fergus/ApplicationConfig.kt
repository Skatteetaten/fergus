package no.skatteetaten.aurora.fergus

import org.openapitools.client.RFC3339DateFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone

@Configuration
class ApplicationConfig {
    @Bean
    fun dateFormat(
        @Value("\${spring.jackson.date-format:}") dateFormatString: String? = null,
        @Value("\${spring.jackson.time-zone:}") timeZone: String? = null,
    ): DateFormat {
        val dateFormat: DateFormat = dateFormatString?.let { SimpleDateFormat(it) } ?: RFC3339DateFormat()
        dateFormat.timeZone = TimeZone.getTimeZone(timeZone ?: "UTC")

        return dateFormat
    }
}
