plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
    signing
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.flutterjunction.formflow.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])
                    artifactId = "formflow-compose"
                    groupId = "com.flutterjunction.formflow"
                    version = project.version.toString()

                }
            }
        }
    }

}
signing {
    val key = System.getenv("SIGNING_KEY")
    val pass = System.getenv("SIGNING_PASSWORD")

    if (!key.isNullOrBlank() && !pass.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pass)
        sign(publishing.publications)
    } else {
        logger.lifecycle("Signing disabled: SIGNING_KEY/SIGNING_PASSWORD not provided.")
    }
}
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = "com.flutterjunction.formflow",
        artifactId = "formflow-compose",
        version = project.version.toString()
    )

    pom {
        name.set("FormFlow Compose")
        description.set("Jetpack Compose adapters for FormFlow.")
        url.set("https://github.com/nbnD/formflow")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        scm { url.set("https://github.com/nbnD/formflow") }
        developers {
            developer {
                id.set("nbnD")
                name.set("Nabin Khanal")
            }
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    api(project(":formflow-core"))
    // Compose runtime (ONLY runtime, no UI widgets)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.collect)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

}