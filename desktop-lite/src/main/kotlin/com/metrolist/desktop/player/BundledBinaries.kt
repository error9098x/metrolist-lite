package com.metrolist.desktop.player

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Locates the `mpv` and `yt-dlp` binaries bundled inside the packaged macOS app.
 *
 * jpackage sets the system property `jpackage.app-path` to the launcher executable, e.g.
 * `/Applications/Metrolist Lite.app/Contents/MacOS/Metrolist Lite`. The bundled binaries are added
 * via `--app-content bin`, landing under `Contents/app/bin` (a few candidates are checked).
 *
 * Returns null when running unpackaged (e.g. `./gradlew run`), so callers fall back to PATH/Homebrew.
 */
object BundledBinaries {

    val dir: File? by lazy {
        val appPath = System.getProperty("jpackage.app-path") ?: return@lazy null
        val contents = File(appPath).parentFile?.parentFile ?: return@lazy null // .../Contents
        listOf("app/bin", "Resources/bin", "bin", "MacOS/bin")
            .map { File(contents, it) }
            .firstOrNull { File(it, "mpv").exists() || File(it, "yt-dlp").exists() }
    }

    fun path(name: String): String? = dir?.let { File(it, name) }?.takeIf { it.canExecute() }?.absolutePath

    @Volatile private var prepared = false

    /** First-run prep: clear Gatekeeper quarantine and ensure the bundled binaries are executable. */
    @Synchronized
    fun ensure() {
        if (prepared) return
        prepared = true
        val d = dir ?: return
        runCatching {
            ProcessBuilder("xattr", "-dr", "com.apple.quarantine", d.absolutePath)
                .start().waitFor(5, TimeUnit.SECONDS)
        }
        d.walkTopDown().forEach { f -> if (f.isFile) runCatching { f.setExecutable(true) } }
    }
}
