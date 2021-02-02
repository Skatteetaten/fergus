plugins {
    id("idea")
    id("java")
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
}
