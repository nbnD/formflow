plugins {
    `java-library`
    `maven-publish`
    signing
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
            groupId = "com.flutterjunction.formflow"
            version = project.version.toString()
            pom {
                name.set("FormFlow Core")
                description.set("UI-agnostic form state, validation, and submission engine")
                url.set("https://github.com/nbnD/formflow")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("nbnD")
                        name.set("Rakshya Khanal")
                    }
                }

                scm {
                    url.set("https://github.com/nbnD/formflow")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

}
