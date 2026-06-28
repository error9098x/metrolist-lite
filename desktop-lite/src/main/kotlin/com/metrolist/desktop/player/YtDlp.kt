package com.metrolist.desktop.player

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Locates the `yt-dlp` binary. mpv's bundled ytdl_hook uses yt-dlp to turn a YouTube watch URL into
 * a playable, poToken-valid audio stream — the reliable path now that WEB stream URLs are gated.
 *
 * GUI apps on macOS often launch without Homebrew on PATH, so we resolve an absolute path and hand
 * it to mpv explicitly (`ytdl_hook-ytdl_path`).
 */
object YtDlp {
    val path: String? by lazy { resolve() }

    fun isAvailable(): Boolean = path != null

    private fun resolve(): String? {
        System.getenv("METROLIST_YTDLP")?.let { if (File(it).canExecute()) return it }

        runCatching {
            val which = ProcessBuilder("/bin/sh", "-c", "command -v yt-dlp").start()
            val out = which.inputStream.bufferedReader().readText().trim()
            which.waitFor(2, TimeUnit.SECONDS)
            if (out.isNotBlank() && File(out).canExecute()) return out
        }

        return listOf(
            "/opt/homebrew/bin/yt-dlp",
            "/usr/local/bin/yt-dlp",
            "/usr/bin/yt-dlp",
        ).firstOrNull { File(it).canExecute() }
    }
}
