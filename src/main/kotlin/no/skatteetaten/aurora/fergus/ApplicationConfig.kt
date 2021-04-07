package no.skatteetaten.aurora.fergus

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import no.skatteetaten.aurora.fergus.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.fergus.security.SharedSecretReader
import no.skatteetaten.aurora.fergus.service.StorageGridServiceReactive
import org.openapitools.client.ApiClient
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.S3Api
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

    @Bean
    fun storageGridService(webClient: WebClient) = StorageGridServiceReactive(webClient)

    @Bean
    fun storageGridS3Api(storageGridApiClient: ApiClient): S3Api = S3Api(storageGridApiClient)

    @Bean
    fun storageGridAuthApi(storageGridApiClient: ApiClient): AuthApi = AuthApi(storageGridApiClient)
}
