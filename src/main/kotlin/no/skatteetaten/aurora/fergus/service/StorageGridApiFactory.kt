package no.skatteetaten.aurora.fergus.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import org.openapitools.client.ApiClient
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.ContainersApi
import org.openapitools.client.api.GroupsApi
import org.openapitools.client.api.S3Api
import org.openapitools.client.api.UsersApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import java.text.DateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Component
class StorageGridApiFactory(
    @Value("\${fergus.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${fergus.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${fergus.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    private val objectMapper: ObjectMapper,
    private val builder: WebClient.Builder,
    private val dateFormat: DateFormat,
    @Value("\${integrations.storagegrid.url}") val storageGridUrl: String
) {

    fun storageGridAuthApi(): AuthApi = AuthApi(createStorageGridApiClient())

    fun storageGridContainersApi(token: String): ContainersApi {
        val apiClient = createStorageGridApiClient()
        apiClient.setBearerToken(token)
        return ContainersApi(apiClient)
    }

    fun storageGridS3Api(token: String): S3Api {
        val apiClient = createStorageGridApiClient()
        apiClient.setBearerToken(token)
        return S3Api(apiClient)
    }

    fun storageGridGroupsApi(token: String): GroupsApi {
        val apiClient = createStorageGridApiClient()
        apiClient.setBearerToken(token)
        return GroupsApi(apiClient)
    }

    fun storageGridUsersApi(token: String): UsersApi {
        val apiClient = createStorageGridApiClient()
        apiClient.setBearerToken(token)
        return UsersApi(apiClient)
    }

    fun createStorageGridApiClient(): ApiClient {
        val client = ApiClient(builder.init().build(), objectMapper, dateFormat)
        client.basePath = "$storageGridUrl${client.basePath.substringAfter("localhost")}"

        return client
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
            .clientConnector(clientConnector(true))

    fun clientConnector(ssl: Boolean = false): ReactorClientHttpConnector {
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
            val sslContext: SslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            return ReactorClientHttpConnector(httpClient.secure { it.sslContext(sslContext) })
        }

        return ReactorClientHttpConnector(httpClient)
    }
}
