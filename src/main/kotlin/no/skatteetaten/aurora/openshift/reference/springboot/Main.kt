@file:JvmName("Main")
package no.skatteetaten.aurora.openshift.reference.springboot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenshiftReferenceSpringbootServerKotlinApplication

fun main(args: Array<String>) {
    runApplication<OpenshiftReferenceSpringbootServerKotlinApplication>(*args)
}
