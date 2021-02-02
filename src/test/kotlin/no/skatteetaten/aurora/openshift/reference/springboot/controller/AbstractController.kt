package no.skatteetaten.aurora.openshift.reference.springboot.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(value = [RestDocumentationExtension::class])
abstract class AbstractController {

    @Autowired
    protected lateinit var context: WebApplicationContext

    protected lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        val configuration: MockMvcRestDocumentationConfigurer =
            MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(configuration)
            .build()
    }
}
