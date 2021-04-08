package no.skatteetaten.aurora.fergus.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import no.skatteetaten.aurora.fergus.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.fergus.security.SharedSecretReader
import org.openapitools.client.ApiClient
import org.openapitools.client.RFC3339DateFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Configuration
class Swagger(
    @Value("\${fergus.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${fergus.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${fergus.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    private val sharedSecretReader: SharedSecretReader,
    private val objectMapper: ObjectMapper,
) {
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
    fun storageGridApiClient(
        builder: WebClient.Builder,
        dateFormat: DateFormat,
        @Value("\${integrations.storagegrid.url}") storageGridUrl: String,
    ): ApiClient {
        val client = ApiClient(builder.init().build(), objectMapper, dateFormat)
        client.basePath = "$storageGridUrl${client.basePath.substringAfter("localhost")}"
        client.setApiKey(sharedSecretReader.secret)
        client.setApiKeyPrefix(HEADER_AURORA_TOKEN)

        return client
    }

    fun WebClient.Builder.init() =
        this.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(
                ExchangeFilterFunction.ofRequestProcessor {
                    logger.debug {
                        val bearer = it.headers()[AUTHORIZATION]?.firstOrNull()?.let { token ->
                            val t = token.substring(0, min(token.length, 11)).replace("Bearer", "")
                            "bearer=$t"
                        } ?: ""
                        "HttpRequest method=${it.method()} url=${it.url()} $bearer"
                    }
                    it.toMono()
                }
            )
            .clientConnector(clientConnector())

    fun clientConnector(ssl: Boolean = false): ReactorClientHttpConnector {
        val httpClient =
            HttpClient.create().compress(true)
                .tcpConfiguration {
                    it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                        .doOnConnected { connection ->
                            connection.addHandlerLast(ReadTimeoutHandler(readTimeout, MILLISECONDS))
                            connection.addHandlerLast(WriteTimeoutHandler(writeTimeout, MILLISECONDS))
                        }
                }

        if (ssl) {
            val sslProvider = SslProvider.builder().sslContext(
                SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
            ).defaultConfiguration(SslProvider.DefaultConfigurationType.NONE).build()
            httpClient.tcpConfiguration {
                it.secure(sslProvider)
            }
        }

        return ReactorClientHttpConnector(httpClient)
    }
}
