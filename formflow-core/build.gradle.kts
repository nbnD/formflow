plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}


kotlin {
    explicitApi()
}
publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "formflow-core"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

}
