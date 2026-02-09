import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
//    withSourcesJar()
//    withJavadocJar()
}

kotlin { explicitApi() }

mavenPublishing {
//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
       publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease =false)
       signAllPublications()


    coordinates(
        groupId = "com.flutterjunction",
        artifactId = "formflow-core",
        version = project.version.toString()
    )

    pom {
        name.set("FormFlow Core")
        description.set("UI-agnostic form state, validation, and submission engine.")
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
                name.set("Nabin Dhakal")
            }
        }
        scm {
            url.set("https://github.com/nbnD/formflow")
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}
