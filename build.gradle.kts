import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("jvm") version "2.4.10"
}

group = "com.example.bag"
version = "0.1.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.200-stable")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val releaseResourcesDir = layout.buildDirectory.dir("release-resources")

val processReleaseResources = tasks.register<ProcessResources>("processReleaseResources") {
    from("src/release/resources")
    into(releaseResourcesDir)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(mapOf("version" to version))
    }
}

val releaseJar = tasks.register<Jar>("releaseJar") {
    group = "build"
    description = "Builds the self-contained distributable bag.jar."
    archiveFileName.set("bag.jar")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(processReleaseResources)

    from(sourceSets.main.get().output) {
        exclude("plugin.yml")
    }
    from(releaseResourcesDir) {
        include("plugin.yml")
    }
    from({
        configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file else zipTree(file)
        }
    }) {
        exclude(
            "META-INF/MANIFEST.MF",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/versions/**/MANIFEST.MF",
            "module-info.class",
            "META-INF/versions/**/module-info.class",
        )
    }
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

val dist = tasks.register<Sync>("dist") {
    group = "build"
    description = "Copies the thin and self-contained release jars to dist/."
    dependsOn(tasks.jar, releaseJar)
    into(layout.projectDirectory.dir("dist"))
    from(tasks.jar)
    from(releaseJar)
}

tasks.test {
    useJUnitPlatform()
}
