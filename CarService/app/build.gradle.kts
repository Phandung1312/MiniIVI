import java.io.File
import java.util.Properties
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.application)
}

val signingPropertiesFile = rootProject.file("../CarSystemUI/signing.properties")
val signingProperties = Properties().apply {
    check(signingPropertiesFile.isFile) {
        "CarSystemUI/signing.properties is required so CarService uses the same platform certificate"
    }
    signingPropertiesFile.inputStream().use(::load)
}

fun signingProperty(name: String): String? =
    signingProperties.getProperty(name)?.takeIf { it.isNotBlank() }

check(listOf(
    "storeFile", "storePassword", "keyAlias", "keyPassword",
).all { signingProperty(it) != null }) {
    "CarSystemUI/signing.properties must define storeFile, storePassword, keyAlias, and keyPassword"
}

val platformStoreFile = File(signingProperty("storeFile")!!).let { configured ->
    if (configured.isAbsolute) configured else File(signingPropertiesFile.parentFile, configured.path)
}
check(platformStoreFile.isFile) {
    "Platform keystore does not exist at ${platformStoreFile.absolutePath}"
}

android {
    namespace = "com.miniivi.car.service"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.miniivi.car.service"
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
        debug {
            signingConfig = signingConfigs.getByName("platform")
        }
        release {
            signingConfig = signingConfigs.getByName("platform")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":car-service-api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    implementation(files("libs/framework/miniivi-framework-adapter.jar"))
    compileOnly(files("libs/framework/android.car.jar"))
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}

val frameworkStubsSource = layout.projectDirectory.dir("framework-stubs/src/main/java")
val frameworkStubsClasses = layout.buildDirectory.dir("framework-stubs/classes")
val frameworkStubsJar = layout.projectDirectory.file("libs/framework/miniivi-framework-stubs.jar")
val frameworkAdapterSource = layout.projectDirectory.dir("framework-adapter/src/main/java")
val frameworkAdapterClasses = layout.buildDirectory.dir("framework-adapter/classes")
val frameworkAdapterJar = layout.projectDirectory.file("libs/framework/miniivi-framework-adapter.jar")
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val sdkDirectory = localProperties.getProperty("sdk.dir")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: System.getenv("ANDROID_HOME")
    ?: error("sdk.dir or ANDROID_SDK_ROOT is required to compile framework stubs")
val androidJar = File(sdkDirectory, "platforms/android-36.1/android.jar")

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
