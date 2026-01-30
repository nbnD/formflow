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
                        name.set("Apache License")
                        url.set("http://www.apache.org/licenses/")
                    }
                }

                developers {
                    developer {
                        id.set("nbnD")
                        name.set("Nabin Dhakal")
                    }
                }

                scm {
                    url.set("https://github.com/nbnD/formflow")
                }
            }
        }
    }
}
afterEvaluate {
    signing {
        val key = System.getenv("SIGNING_KEY")
        val pass = System.getenv("SIGNING_PASSWORD")

        if (!key.isNullOrBlank() && !pass.isNullOrBlank()) {
            useInMemoryPgpKeys(key, pass)
            sign(publishing.publications["release"])
        } else {
            // CI/builds without publishing shouldn't fail
            logger.lifecycle("Signing disabled: SIGNING_KEY/SIGNING_PASSWORD not provided.")
        }
    }

}
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

}
