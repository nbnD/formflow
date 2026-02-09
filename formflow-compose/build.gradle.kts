import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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


}


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL,automaticRelease = false)
    signAllPublications()

    coordinates(
        groupId = "com.flutterjunction",
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
                name.set("Nabin Dhakal")
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