plugins {
    id("idea")
    id("java")
    id("org.openapi.generator") version "5.2.0"
    id("no.skatteetaten.gradle.aurora") version("4.5.4")
}

aurora {
    useKotlinDefaults
    useSpringBootDefaults

    useSpringBoot {
        useWebFlux
        useCloudContract
    }
}

dependencies {
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.7")

    // Swagger
    implementation("org.openapitools:jackson-databind-nullable:0.2.3")
    implementation("io.swagger:swagger-annotations:1.6.6")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // MockWebServer
    testImplementation("no.skatteetaten.aurora:mockwebserver-extensions-kotlin:1.3.1")

    testImplementation("com.ninja-squad:springmockk:3.1.1")
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
