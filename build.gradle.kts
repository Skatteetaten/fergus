plugins {
    id("idea")
    id("java")
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("no.skatteetaten.gradle.aurora") version("4.2.2")
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
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.2")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.2")
}
