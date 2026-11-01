// bag 构建脚本。
// 普通构建（jar/build）产出开发包；dist 任务同时产出轻量包和自包含包。
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("jvm") version "2.4.10"
}

group = "com.example.bag"
version = "0.2.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven(url = "https://repo.momirealms.net/releases/") {
        name = "momirealms"
    }
}

dependencies {
    // 服务端/运行时由 Paper 或 CE 插件提供，不能打进普通开发包。
    compileOnly("io.papermc.paper:paper-api:26.3.build.200-stable")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    compileOnly("net.momirealms:craft-engine-core:26.9")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9")
    // 单元测试依赖只存在于测试配置，不会进入插件 JAR。
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

// 将构建版本写入开发包的 plugin.yml；不会修改源代码中的模板。
tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val releaseResourcesDir = layout.buildDirectory.dir("release-resources")

// 自包含发布包使用 src/release/resources 中的 plugin.yml（不含 libraries），
// 因为它会把 Kotlin stdlib 直接放进 JAR；开发包仍保留 libraries 方案。
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

    // 主输出中的 plugin.yml 替换为 release 版本；config.yml/classes 保留。
    from(sourceSets.main.get().output) {
        exclude("plugin.yml")
    }
    from(releaseResourcesDir) {
        include("plugin.yml")
    }
    // 把 runtimeClasspath（Kotlin stdlib 等）展开进 JAR；Paper API/MiniMessage
    // 是 compileOnly，不会出现在这里。签名文件必须排除，否则合并后会非法。
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

// dist 只负责发布产物收集：普通开发 JAR + 自包含 bag.jar。
// dist/ 已在 .gitignore 中，不提交构建产物。
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
