package no.skatteetaten.aurora.openshift.reference.springboot.service

import org.springframework.stereotype.Component

@Component
class SometimesFailingService {

    fun performOperationThatMayFail(): Boolean {

        val sleepTime = (Math.random() * 1000).toLong()
        Thread.sleep(sleepTime)
        return sleepTime % 2 == 0L
    }
}
