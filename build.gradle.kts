plugins {
    id("idea")
    id("java")
    id("org.openapi.generator") version "5.1.0"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("no.skatteetaten.gradle.aurora") version("4.3.12")
    id("net.linguica.maven-settings") version "0.5"
}

repositories {
    jcenter()
    mavenCentral()
}

aurora {
    useAuroraDefaults
    useKotlin {
        useKtLint
    }
    useSpringBoot {
        useWebFlux
        useCloudContract
    }

    features {
        checkstylePlugin = false
    }
}

dependencies {
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.2")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.2")

    // Swagger
    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
    implementation("io.swagger:swagger-annotations:1.6.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // MockWebServer
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.1.6")

    testImplementation("com.ninja-squad:springmockk:2.0.1")
}

openApiGenerate {
    inputSpec.set("src/main/resources/swagger/storagegrid-api.yml")
    outputDir.set("$buildDir/storagegrid-api-swagger")
    generatorName.set("java")
    library.set("webclient")
    configFile.set("src/main/resources/swagger/config_storagegrid.json")
}

sourceSets {
    main {
        java.srcDirs(
            "$buildDir/storagegrid-api-swagger/src/main/java"
        )
    }
}

tasks {
    "compileKotlin" {
        dependsOn("openApiGenerate")
    }
}
