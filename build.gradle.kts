plugins {
    id("java")
    // Add the application plugin so we can run with ./gradlew run
    id("application")
    // JavaFX Gradle plugin to pull correct platform-specific JavaFX artifacts
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // No direct JavaFX dependencies needed here when using the javafx { } block below unless you add extra modules
    // implementation("org.openjfx:javafx-controls:22.0.1") // handled by plugin
}

application {
    // Fully qualified main class (non-modular project)
    mainClass.set("org.example.Main")
}

javafx {
    version = "22.0.1"
    modules = listOf("javafx.controls") // pulls transitive (graphics, base) + native libs for your OS
}

tasks.test {
    useJUnitPlatform()
}