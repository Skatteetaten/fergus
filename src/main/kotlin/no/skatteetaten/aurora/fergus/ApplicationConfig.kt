package no.skatteetaten.aurora.fergus

import org.openapitools.client.ApiClient
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.S3Api
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {
    @Bean
    fun storageGridS3Api(storageGridApiClient: ApiClient): S3Api = S3Api(storageGridApiClient)

    @Bean
    fun storageGridAuthApi(storageGridApiClient: ApiClient): AuthApi = AuthApi(storageGridApiClient)
}
