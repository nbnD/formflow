// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.binary.compatibility)
    alias(libs.plugins.vanniktech.maven.publish)

}

allprojects {
    // Maven Central coordinates (namespace verified)
    group = "com.flutterjunction.formflow"
    version = "0.1.4-alpha"
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(21)
        }
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>("kotlin") {
            jvmToolchain(21)
        }
    }

}

// TODO: Upgrade to Kotlin 2.2.x once binary-compatibility-validator supports newer Kotlin metadata
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

apiValidation {
    ignoredProjects += listOf("sample-compose")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = "com.flutterjunction.formflow",
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
        scm {
            url.set("https://github.com/nbnD/formflow")
        }
        developers {
            developer {
                id.set("nbnD")
                name.set("Nabin Dhakal")
            }
        }
    }
}
