import org.hidetake.gradle.swagger.generator.GenerateSwaggerCode

plugins {
    id("idea")
    id("java")
    id("org.hidetake.swagger.generator") version "2.18.2"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("no.skatteetaten.gradle.aurora") version("4.2.2")
}

repositories {
    jcenter()
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
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.2")
    swaggerCodegen("io.swagger.codegen.v3:swagger-codegen-cli:3.0.25")
}

swaggerSources {
    create("storagegrid").apply {
        setInputFile(file("src/main/resources/swagger/storagegrid-api.yml"))
        code(
            closureOf<GenerateSwaggerCode> {
                language = "kotlin-client"
            }
        )
    }
}

tasks {
    val generateSwaggerCode by getting(GenerateSwaggerCode::class)

    @Suppress("UNUSED_VARIABLE")
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        dependsOn(listOf(generateSwaggerCode))
    }
}
