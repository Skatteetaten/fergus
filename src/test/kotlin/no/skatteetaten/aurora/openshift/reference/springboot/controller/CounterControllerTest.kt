package no.skatteetaten.aurora.openshift.reference.springboot.controller

import no.skatteetaten.aurora.openshift.reference.springboot.controllers.CounterController
import no.skatteetaten.aurora.openshift.reference.springboot.service.CounterDatabaseService
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CounterController::class)
class CounterControllerTest : AbstractController() {

    @MockBean
    lateinit var counterDatabaseService: CounterDatabaseService

    @Test
    fun sample() {

        given(counterDatabaseService.getAndIncrementCounter()).willReturn(5L)

        mvc.perform(get("/api/counter"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value", equalTo(5)))
            .andDo(
                document(
                    "counter-get",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("value").type(JsonFieldType.NUMBER).description("The current value of the counter")
                    )
                )
            )
    }
}
