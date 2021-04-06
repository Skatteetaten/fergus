package no.skatteetaten.aurora.fergus

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
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.S3Api
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import java.text.DateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class ServiceTypes {
    STORAGEGRID,
    STORAGEGRID_S3,
    STORAGEGRID_AUTH,
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TargetService(val value: ServiceTypes)

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.storagegrid.url")
class RequiresStorageGrid

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig(
    @Value("\${fergus.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${fergus.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${fergus.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    @Value("\${spring.application.name}") val applicationName: String,
    private val objectMapper: ObjectMapper,
    private val sharedSecretReader: SharedSecretReader
) {

    @ConditionalOnBean(RequiresStorageGrid::class)
    @Bean
    @TargetService(ServiceTypes.STORAGEGRID)
    fun webClientStorageGrid(
        @Value("\${integrations.storagegrid.url}") storageGridUrl: String,
        builder: WebClient.Builder
    ) =
        builder.init().baseUrl(storageGridUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}").build()

    @ConditionalOnBean(RequiresStorageGrid::class)
    @Bean
    @TargetService(ServiceTypes.STORAGEGRID_S3)
    fun storageGridS3Api(
        @Value("\${integrations.storagegrid.url}") storageGridUrl: String,
        builder: WebClient.Builder
    ): S3Api {
        val webClient = builder
            .init()
            .baseUrl(storageGridUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .build()
        val dateFormat: DateFormat = RFC3339DateFormat()
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return S3Api(ApiClient(webClient, objectMapper, dateFormat))
    }

    @ConditionalOnBean(RequiresStorageGrid::class)
    @Bean
    @TargetService(ServiceTypes.STORAGEGRID_AUTH)
    fun storageGridAuthApi(
        @Value("\${integrations.storagegrid.url}") storageGridUrl: String,
        builder: WebClient.Builder
    ): AuthApi {
        val webClient = builder
            .init()
            .baseUrl(storageGridUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .build()
        val dateFormat: DateFormat = RFC3339DateFormat()
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return AuthApi(ApiClient(webClient, objectMapper, dateFormat))
    }

    fun WebClient.Builder.init() =
        this.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(
                ExchangeFilterFunction.ofRequestProcessor {
                    logger.debug {
                        val bearer = it.headers()[HttpHeaders.AUTHORIZATION]?.firstOrNull()?.let { token ->
                            val t = token.substring(0, min(token.length, 11)).replace("Bearer", "")
                            "bearer=$t"
                        } ?: ""
                        "HttpRequest method=${it.method()} url=${it.url()} $bearer"
                    }
                    it.toMono()
                }
            )
            .clientConnector(clientConnector())

    private fun clientConnector(ssl: Boolean = false): ReactorClientHttpConnector {
        val httpClient =
            HttpClient.create().compress(true)
                .tcpConfiguration {
                    it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                        .doOnConnected { connection ->
                            connection.addHandlerLast(ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                            connection.addHandlerLast(WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
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
