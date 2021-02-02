package no.skatteetaten.aurora.openshift.reference.springboot.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController {

    @GetMapping("/")
    fun index() = "redirect:/docs/index.html"
}
