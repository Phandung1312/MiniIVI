import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingPropertiesFile = rootProject.file("../CarLauncher/signing.properties")
val signingProperties = Properties().apply {
    check(signingPropertiesFile.isFile) {
        "CarLauncher/signing.properties is required so MiniIviMaps uses the same platform certificate"
    }
    signingPropertiesFile.inputStream().use(::load)
}

fun signingProperty(name: String): String? =
    signingProperties.getProperty(name)?.takeIf { it.isNotBlank() }

check(listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
    signingProperty(it) != null
}) {
    "CarLauncher/signing.properties must define storeFile, storePassword, keyAlias, and keyPassword"
}

val platformStoreFile = File(signingProperty("storeFile")!!).let { configured ->
    if (configured.isAbsolute) configured else File(signingPropertiesFile.parentFile, configured.path)
}
check(platformStoreFile.isFile) {
    "Platform keystore does not exist at ${platformStoreFile.absolutePath}"
}

android {
    namespace = "com.miniivi.maps"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.miniivi.maps"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("platform") {
            storeFile = platformStoreFile
            storePassword = signingProperty("storePassword")
            keyAlias = signingProperty("keyAlias")
            keyPassword = signingProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("platform") }
        release {
            signingConfig = signingConfigs.getByName("platform")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":contract"))
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
