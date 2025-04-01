repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.1.0"
}

group = "org.jordynsblog"
version = "1.0-SNAPSHOT"

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("io.ktor:ktor-client-core:3.1.0")
    implementation("io.ktor:ktor-client-cio:3.1.0")
    implementation("com.github.kittinunf.result:result:5.6.0")
    implementation("io.github.aakira:napier:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation(platform("org.kotlincrypto.hash:bom:0.7.0"))
    implementation("org.kotlincrypto.hash:md")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
