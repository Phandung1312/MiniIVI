import java.util.Properties
import java.io.File
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Put ROM/AOSP build stubs in app/libs/framework. They are compile-time only;
    // the corresponding classes are supplied by the system image at runtime.
    compileOnly(fileTree("libs/framework") { include("*.jar") })
    implementation(files("libs/framework/miniivi-framework-adapter.jar"))
    implementation(project(":boot-brand"))
    implementation(project(":car-service-client"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

// The public SDK omits a few SystemUI-only APIs. Build a tiny compile-only JAR that
// declares only the methods this module calls; Android provides their implementation
// from framework.jar at runtime.
val frameworkStubsSource = layout.projectDirectory.dir("framework-stubs/src/main/java")
val frameworkStubsClasses = layout.buildDirectory.dir("framework-stubs/classes")
val frameworkStubsJar = layout.projectDirectory.file("libs/framework/miniivi-framework-stubs.jar")
val frameworkAdapterSource = layout.projectDirectory.dir("framework-adapter/src/main/java")
val frameworkAdapterClasses = layout.buildDirectory.dir("framework-adapter/classes")
val frameworkAdapterJar = layout.projectDirectory.file("libs/framework/miniivi-framework-adapter.jar")
val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use(::load)
}
val androidJar = File(
    localProperties.getProperty("sdk.dir") ?: error("sdk.dir is required to compile framework stubs"),
    "platforms/android-36.1/android.jar",
)

val compileFrameworkStubs by tasks.registering(JavaCompile::class) {
    source = fileTree(frameworkStubsSource.asFile)
    classpath = files(androidJar)
    destinationDirectory.set(frameworkStubsClasses)
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

val packageFrameworkStubs by tasks.registering(Jar::class) {
    dependsOn(compileFrameworkStubs)
    from(frameworkStubsClasses)
    destinationDirectory.set(frameworkStubsJar.asFile.parentFile)
    archiveFileName.set(frameworkStubsJar.asFile.name)
}

val compileFrameworkAdapter by tasks.registering(JavaCompile::class) {
    dependsOn(packageFrameworkStubs)
    source = fileTree(frameworkAdapterSource.asFile)
    classpath = files(frameworkStubsJar, androidJar)
    destinationDirectory.set(frameworkAdapterClasses)
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

val packageFrameworkAdapter by tasks.registering(Jar::class) {
    dependsOn(compileFrameworkAdapter)
    from(frameworkAdapterClasses)
    destinationDirectory.set(frameworkAdapterJar.asFile.parentFile)
    archiveFileName.set(frameworkAdapterJar.asFile.name)
}

tasks.named("preBuild").configure {
    dependsOn(packageFrameworkAdapter)
}
