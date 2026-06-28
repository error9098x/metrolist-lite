package com.metrolist.desktop.download

import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.lyrics.SavedLyrics
import com.metrolist.desktop.lyrics.toState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistent index of downloaded songs under ~/.metrolist-lite/downloads, plus the audio and .lrc
 * files themselves. Thread-safe; the JSON index is rewritten on every change.
 */
class DownloadStore {
    val dir: File = File(System.getProperty("user.home"), ".metrolist-lite/downloads").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val items = LinkedHashMap<String, DownloadedSong>()

    init { load() }

    @Synchronized fun all(): List<DownloadedSong> = items.values.toList()
    @Synchronized fun get(videoId: String): DownloadedSong? = items[videoId]
    fun isDownloaded(videoId: String): Boolean = get(videoId) != null
    fun audioPath(videoId: String): String? = get(videoId)?.audioPath?.takeIf { File(it).exists() }

    fun lrcText(videoId: String): String? =
        get(videoId)?.lrcPath?.let { p -> File(p).takeIf { it.exists() }?.readText() }

    fun lrcFileFor(videoId: String): File = File(dir, "$videoId.lrc")
    fun lyricsJsonFileFor(videoId: String): File = File(dir, "$videoId.lyrics.json")

    /** Offline word-synced lyrics for a downloaded song, if saved. */
    fun lyricsState(videoId: String): LyricsState? {
        val p = get(videoId)?.lyricsJsonPath ?: return null
        val f = File(p); if (!f.exists()) return null
        return runCatching { json.decodeFromString<SavedLyrics>(f.readText()).toState() }.getOrNull()
    }

    @Synchronized
    fun add(song: DownloadedSong) {
        items[song.videoId] = song
        save()
    }

    @Synchronized
    fun remove(videoId: String) {
        items.remove(videoId)?.let { s ->
            runCatching { File(s.audioPath).delete() }
            s.lrcPath?.let { runCatching { File(it).delete() } }
            s.coverPath?.let { runCatching { File(it).delete() } }
            s.lyricsJsonPath?.let { runCatching { File(it).delete() } }
        }
        save()
    }

    private fun save() {
        runCatching { indexFile.writeText(json.encodeToString(items.values.toList())) }
    }

    private fun load() {
        if (!indexFile.exists()) return
        runCatching {
            json.decodeFromString<List<DownloadedSong>>(indexFile.readText())
                .filter { File(it.audioPath).exists() }
                .forEach { items[it.videoId] = it }
        }
    }
}
