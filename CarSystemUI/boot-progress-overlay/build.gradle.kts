import java.util.Properties
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class StripRroDexTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArchive: RegularFileProperty

    @get:OutputFile
    abstract val outputArchive: RegularFileProperty

    @TaskAction
    fun strip() {
        val input = inputArchive.get().asFile
        val output = outputArchive.get().asFile
        output.parentFile.mkdirs()
        ZipFile(input).use { source ->
            ZipOutputStream(output.outputStream()).use { destination ->
                source.entries().asSequence()
                    .filterNot { it.name.matches(Regex("classes(?:\\d+)?\\.dex")) }
                    .filterNot { it.name.startsWith("META-INF/") }
                    .filterNot { it.name.startsWith("kotlin/") }
                    .forEach { entry ->
                        val copy = ZipEntry(entry.name).apply {
                            time = 0L
                            method = entry.method
                            if (entry.method == ZipEntry.STORED) {
                                size = entry.size
                                compressedSize = entry.size
                                crc = entry.crc
                            }
                        }
                        destination.putNextEntry(copy)
                        source.getInputStream(entry).use { it.copyTo(destination) }
                        destination.closeEntry()
                    }
            }
        }
    }
}

abstract class AlignRroTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val executable: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArchive: RegularFileProperty

    @get:OutputFile
    abstract val outputArchive: RegularFileProperty

    @TaskAction
    fun align() {
        val output = outputArchive.get().asFile
        output.parentFile.mkdirs()
        output.delete()
        execOperations.exec {
            commandLine(
                this@AlignRroTask.executable.get().asFile.absolutePath,
                "-f",
                "4",
                inputArchive.get().asFile.absolutePath,
                output.absolutePath,
            )
        }
    }
}

abstract class SignRroTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val executable: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val keyStore: RegularFileProperty

    @get:Input
    abstract val keyAlias: Property<String>

    @get:Internal
    abstract val storePassword: Property<String>

    @get:Internal
    abstract val keyPassword: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArchive: RegularFileProperty

    @get:OutputFile
    abstract val outputArchive: RegularFileProperty

    @TaskAction
    fun sign() {
        val output = outputArchive.get().asFile
        output.parentFile.mkdirs()
        output.delete()
        execOperations.exec {
            commandLine(
                this@SignRroTask.executable.get().asFile.absolutePath,
                "sign",
                "--ks", keyStore.get().asFile.absolutePath,
                "--ks-key-alias", keyAlias.get(),
                "--ks-pass", "pass:${storePassword.get()}",
                "--key-pass", "pass:${keyPassword.get()}",
                "--out", output.absolutePath,
                inputArchive.get().asFile.absolutePath,
            )
        }
    }
}

abstract class VerifyBootProgressOverlayTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val archive: RegularFileProperty

    @TaskAction
    fun verify() {
        val file = archive.get().asFile
        check(file.isFile) { "Boot-progress RRO was not generated: $file" }
        ZipFile(file).use { zip ->
            check(zip.getEntry("AndroidManifest.xml") != null) { "RRO manifest is missing" }
            check(zip.getEntry("resources.arsc") != null) { "RRO resource table is missing" }
            check(zip.getEntry("resources.arsc").method == ZipEntry.STORED) {
                "RRO resource table must be stored uncompressed"
            }
            check(zip.getEntry("classes.dex") == null) { "RRO must not contain executable code" }
            check(zip.entries().asSequence().none { it.name.startsWith("kotlin/") }) {
                "RRO must not contain Kotlin runtime metadata"
            }
        }
    }
}

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
    namespace = "com.miniivi.bootprogress.overlay"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.miniivi.bootprogress.overlay"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        }
    }

    buildFeatures {
        buildConfig = false
    }
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use(::load)
}
val androidSdk = file(localProperties.getProperty("sdk.dir"))
val buildTools = File(androidSdk, "build-tools/36.0.0")
val zipAlignExecutable = File(buildTools, "zipalign.exe")
val apkSignerExecutable = File(buildTools, "apksigner.bat")
val packagedRro = layout.buildDirectory.file("outputs/apk/release/boot-progress-overlay-release.apk")
val strippedRro = layout.buildDirectory.file("intermediates/boot-progress-overlay/stripped.apk")
val alignedRro = layout.buildDirectory.file("intermediates/boot-progress-overlay/aligned.apk")
val deployableRro = layout.buildDirectory.file("outputs/boot-progress-overlay/MiniIviBootProgressOverlay.apk")

val stripBootProgressOverlayDex by tasks.registering(StripRroDexTask::class) {
    group = "build"
    description = "Removes generated DEX files from the code-free RRO package."
    dependsOn("packageRelease")
    inputArchive.set(packagedRro)
    outputArchive.set(strippedRro)
}

val alignBootProgressOverlay by tasks.registering(AlignRroTask::class) {
    group = "build"
    description = "Aligns the code-free boot-progress RRO."
    dependsOn(stripBootProgressOverlayDex)
    executable.set(zipAlignExecutable)
    inputArchive.set(strippedRro)
    outputArchive.set(alignedRro)
}

val signBootProgressOverlay by tasks.registering(SignRroTask::class) {
    group = "build"
    description = "Signs the deployable boot-progress RRO with the platform certificate."
    dependsOn(alignBootProgressOverlay)
    executable.set(apkSignerExecutable)
    keyStore.set(rootProject.file(signingProperty("storeFile") ?: "missing"))
    keyAlias.set(signingProperty("keyAlias") ?: "missing")
    storePassword.set(signingProperty("storePassword") ?: "missing")
    keyPassword.set(signingProperty("keyPassword") ?: "missing")
    inputArchive.set(alignedRro)
    outputArchive.set(deployableRro)
}

val verifyBootProgressOverlay by tasks.registering(VerifyBootProgressOverlayTask::class) {
    group = "verification"
    description = "Verifies that the boot-progress RRO is code-free and contains its resource table."
    dependsOn(signBootProgressOverlay)
    archive.set(deployableRro)
}

tasks.named("check") {
    dependsOn(verifyBootProgressOverlay)
}

tasks.named("assemble") {
    dependsOn(signBootProgressOverlay)
}
