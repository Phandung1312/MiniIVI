import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val signingPropertiesFile = rootProject.file("signing.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

fun signingProperty(name: String): String? =
    signingProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val platformSigningConfigured = listOf(
    "storeFile", "storePassword", "keyAlias", "keyPassword"
).all { signingProperty(it) != null }

android {
    namespace = "com.android.car.systemui"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.android.car.systemui"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    signingConfigs {
        if (platformSigningConfigured) {
            create("platform") {
                storeFile = rootProject.file(signingProperty("storeFile")!!)
                storePassword = signingProperty("storePassword")
                keyAlias = signingProperty("keyAlias")
                keyPassword = signingProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            if (platformSigningConfigured) {
                signingConfig = signingConfigs.getByName("platform")
            }
        }
        release {
            if (platformSigningConfigured) {
                signingConfig = signingConfigs.getByName("platform")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Put ROM/AOSP build stubs in app/libs/framework. They are compile-time only;
    // the corresponding classes are supplied by the system image at runtime.
    compileOnly(fileTree("libs/framework") { include("*.jar") })

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
