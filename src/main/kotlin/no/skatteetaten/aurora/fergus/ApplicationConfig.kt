package no.skatteetaten.aurora.fergus

import org.openapitools.client.RFC3339DateFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
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

    @Bean
    @Profile("local")
    fun kubernetesLocalKeyStore(): KeyStore? = null

    @Bean
    @Primary
    @Profile("openshift")
    fun kubernetesSSLContext(@Value("\${trust.store}") trustStoreLocation: String): KeyStore =
        KeyStore.getInstance(KeyStore.getDefaultType())?.let { ks ->
            ks.load(FileInputStream(trustStoreLocation), "changeit".toCharArray())
            val fis = FileInputStream("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt")
            CertificateFactory.getInstance("X509").generateCertificates(fis).forEach {
                ks.setCertificateEntry((it as X509Certificate).subjectX500Principal.name, it)
            }
            ks
        } ?: throw Exception("KeyStore getInstance did not return KeyStore")
}
