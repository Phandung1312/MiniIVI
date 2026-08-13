plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":boot-brand"))
    testImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    systemProperty("java.awt.headless", "true")
}

val bootAnimationOutputDirectory = layout.buildDirectory.dir("outputs/bootanimation")
val standardArchive = bootAnimationOutputDirectory.map { it.file("bootanimation.zip") }
val darkArchive = bootAnimationOutputDirectory.map { it.file("bootanimation-dark.zip") }

val generateBootAnimations by tasks.registering(JavaExec::class) {
    group = "boot animation"
    description = "Generates the standard and dark MiniIVI boot animation archives."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.miniivi.bootanimation.BootAnimationGenerator")
    jvmArgs("-Djava.awt.headless=true")
    args(bootAnimationOutputDirectory.get().asFile.absolutePath)
    outputs.files(standardArchive, darkArchive)
}

val verifyBootAnimations by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Verifies the generated MiniIVI boot animation archives."
    dependsOn(generateBootAnimations)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.miniivi.bootanimation.BootAnimationVerifier")
    jvmArgs("-Djava.awt.headless=true")
    args(
        standardArchive.get().asFile.absolutePath,
        darkArchive.get().asFile.absolutePath,
    )
    inputs.files(standardArchive, darkArchive)
}

tasks.named("assemble") {
    dependsOn(generateBootAnimations)
}

tasks.named("check") {
    dependsOn(verifyBootAnimations)
}
