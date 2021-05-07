@file:JvmName("Main")
package no.skatteetaten.aurora.fergus

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Fergus

fun main(args: Array<String>) {
    runApplication<Fergus>(*args)
}
