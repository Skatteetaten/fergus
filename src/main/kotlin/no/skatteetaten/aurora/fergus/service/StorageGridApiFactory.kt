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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import java.security.KeyStore
import java.text.DateFormat
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory

private val logger = KotlinLogging.logger {}

@Component
class StorageGridApiFactory(
    @Value("\${fergus.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${fergus.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${fergus.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    private val objectMapper: ObjectMapper,
    private val builder: WebClient.Builder,
    private val dateFormat: DateFormat,
    @Value("\${integrations.storagegrid.url}") val storageGridUrl: String,
    @Value("\${fergus.ssl.trustmanagerinsecure}") val useInsecureTrustManager: Boolean,
    @Autowired private val trustStore: KeyStore?
) {

    fun storageGridAuthApi(): AuthApi = AuthApi(createStorageGridApiClient())

    fun storageGridContainersApi(token: String): ContainersApi = ContainersApi(apiClientForToken(token))

    fun storageGridS3Api(token: String): S3Api = S3Api(apiClientForToken(token))

    fun storageGridGroupsApi(token: String): GroupsApi = GroupsApi(apiClientForToken(token))

    fun storageGridUsersApi(token: String): UsersApi = UsersApi(apiClientForToken(token))

    private fun apiClientForToken(token: String): ApiClient {
        val apiClient = createStorageGridApiClient()
        apiClient.setApiKey(token)
        return apiClient
    }

    private fun createStorageGridApiClient(): ApiClient {
        val client = ApiClient(builder.init().build(), objectMapper, dateFormat)

        client.basePath = "$storageGridUrl${client.basePath.substringAfter("localhost")}"

        return client
    }

    private fun WebClient.Builder.init() =
        this.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(
                ExchangeFilterFunction.ofRequestProcessor {
                    logger.debug {
                        "HttpRequest method=${it.method()} url=${it.url()}"
                    }
                    it.toMono()
                }
            )
            .clientConnector(clientConnector(true))

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
            val trustFactory = TrustManagerFactory.getInstance("X509")
            trustFactory.init(trustStore)
            val sslContext: SslContext = SslContextBuilder
                .forClient()
                .trustManager(if (useInsecureTrustManager) InsecureTrustManagerFactory.INSTANCE else trustFactory)
                .build()

            return ReactorClientHttpConnector(httpClient.secure { it.sslContext(sslContext) })
        }

        return ReactorClientHttpConnector(httpClient)
    }
}
