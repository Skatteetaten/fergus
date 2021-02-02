package no.skatteetaten.aurora.openshift.reference.springboot.controller

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.skatteetaten.aurora.AuroraMetrics
import no.skatteetaten.aurora.openshift.reference.springboot.controllers.ErrorHandler
import no.skatteetaten.aurora.openshift.reference.springboot.controllers.ExampleController
import no.skatteetaten.aurora.openshift.reference.springboot.service.SometimesFailingService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class Config {
    @Bean
    fun meterRegistry(): MeterRegistry = Metrics.globalRegistry
}

@WebMvcTest(controllers = [ExampleController::class, ErrorHandler::class])
@Import(value = [Config::class, AuroraMetrics::class])
@AutoConfigureWebClient(registerRestTemplate = true)
@AutoConfigureMockRestServiceServer
class ExampleControllerTest : AbstractController() {

    @Autowired
    private lateinit var server: MockRestServiceServer

    @MockBean
    private lateinit var sometimesFailingService: SometimesFailingService

    @Test
    fun `Example test for documenting the ip endpoint`() {

        server.expect(requestTo("http://httpbin.org/ip"))
            .andRespond(withSuccess("""{ "origin": "154.127.163.2" }""", APPLICATION_JSON))

        mvc.perform(get("/api/example/ip"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "example-ip-get",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("ip")
                            .type(JsonFieldType.STRING)
                            .description("The ip of this service as seen from the Internet")
                    )
                )
            )
    }

    @Test
    fun `Example test for documenting the sometimes endpoint`() {

        given(sometimesFailingService.performOperationThatMayFail()).willReturn(false)

        val apiUrl = "/api/example/sometimes"

        mvc.perform(get(apiUrl))
            .andExpect(status().is5xxServerError)
            .andDo(
                document(
                    "example-sometimes-fail-get",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("errorMessage")
                            .type(JsonFieldType.STRING)
                            .description("The error message describing the error that occurred"),
                        fieldWithPath("cause")
                            .optional()
                            .type(JsonFieldType.STRING)
                            .description("An optional cause description with details of the underlying cause of the error")
                    )
                )
            )

        given(sometimesFailingService.performOperationThatMayFail()).willReturn(true)

        mvc.perform(get(apiUrl))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "example-sometimes-success-get",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("result")
                            .type(JsonFieldType.STRING)
                            .description("The result of a successful operation")
                    )
                )
            )
    }
}
