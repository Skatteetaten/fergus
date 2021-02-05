plugins {
    id("idea")
    id("java")
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("no.skatteetaten.gradle.aurora") version("4.2.0")
}

aurora {
    useAuroraDefaults
    useKotlin {
        useKtLint
    }
    useSpringBoot {
        useCloudContract
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
