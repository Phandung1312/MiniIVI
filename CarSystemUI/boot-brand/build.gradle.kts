plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
