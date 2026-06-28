@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "Metrolist"

// Metrolist Lite (macOS desktop) builds ONLY the desktop module. It reuses the
// innertube / lrclib / betterlyrics *source* directly (see desktop-lite/build.gradle.kts),
// so the Android library modules and the Android :app are intentionally NOT included here.
// This keeps the build Android-SDK-free (important for CI). Those sources stay in the tree
// for reference and upstream sync; to build the original Android app, restore the includes:
//   include(":app", ":innertube", ":kugou", ":lrclib", ":lastfm", ":betterlyrics", ":shazamkit", ":paxsenix")
include(":desktop-lite")

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that Metrolist and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in innertube/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.newpipe.extractor)
// To:
//      implementation("com.github.teamnewpipe:NewPipeExtractor")
//includeBuild("../NewPipeExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.teamnewpipe:NewPipeExtractor")).using(project(":extractor"))
//    }
//}
