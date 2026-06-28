import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * desktop-lite: macOS personal-use MVP for Metrolist.
 *
 * Design goals (KISS / SOLID):
 *  - Pure Kotlin/JVM `application` module. No Android plugin, no Compose.
 *  - Reuses the `innertube` *source* directly (it has zero Android imports) instead of
 *    depending on the Android `:innertube` library project, which a JVM module cannot consume.
 *  - The only Android-only dependency used by innertube is Jake Wharton's Timber. We replace it
 *    with a tiny no-Android shim under `src/main/kotlin/timber/log/Timber.kt`.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

// Compile innertube's pure-Kotlin source in-place. No churn to the Android module.
sourceSets {
    named("main") {
        java.srcDir("../innertube/src/main/kotlin")
        // LRCLIB synced-lyrics client (pure Kotlin, no Android).
        java.srcDir("../lrclib/src/main/kotlin")
        // BetterLyrics: word-by-word (TTML) synced lyrics (pure Kotlin/JVM XML, no Android).
        java.srcDir("../betterlyrics/src/main/kotlin")
    }
}

kotlin {
    // We run on whatever JDK Gradle uses (Java 21+). Target 21 bytecode to match innertube.
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // --- innertube runtime deps (mirrors innertube/build.gradle.kts, minus Android-only bits) ---
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio) // lrclib uses the CIO engine
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutinesGuava.get()}")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // NewPipe extractor fork: resolves play URLs (signature/n-param/poToken) without Android.
    // Matches the exact ref the Android innertube module compiles against.
    implementation("com.github.MetrolistGroup:MetrolistExtractor:f0a00f5") {
        exclude(group = "com.google.protobuf")
    }
}

// The NewPipe extractor fork can resolve to the same jar filename twice on the runtime classpath;
// the `application` distribution tasks copy it into lib/ and would otherwise fail on the collision.
tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
    mainClass.set("com.metrolist.desktop.MainKt")
    // Ensure a real AWT/Swing app on macOS (not headless) with a sensible menu-bar name.
    applicationDefaultJvmArgs = listOf(
        "-Djava.awt.headless=false",
        "-Dapple.awt.application.name=Metrolist Lite",
    )
}
