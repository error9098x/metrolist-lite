package com.metrolist.desktop.download

import com.metrolist.desktop.lyrics.LyricsService
import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.lyrics.toSaved
import com.metrolist.desktop.model.Song
import com.metrolist.desktop.player.YtDlp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

/**
 * Downloads a song's best audio stream with yt-dlp, plus its synced LRC lyrics, for offline use.
 * Progress is parsed from yt-dlp's `--newline` output. Listeners are notified on the EDT.
 */
class DownloadManager(
    private val store: DownloadStore,
    private val onlineLyrics: LyricsService,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val pool = Executors.newFixedThreadPool(2) { r -> Thread(r, "downloader").apply { isDaemon = true } }
    private val active = ConcurrentHashMap<String, DownloadStatus>()
    private val listeners = CopyOnWriteArrayList<DownloadListener>()
    private val progressPattern = Regex("""\[download]\s+(\d+(?:\.\d+)?)%""")

    fun addListener(l: DownloadListener) { listeners += l }

    fun remove(videoId: String) {
        store.remove(videoId)
        active.remove(videoId)
        notifyChanged(videoId, DownloadStatus.None)
        notifyLibrary()
    }

    fun statusOf(videoId: String): DownloadStatus = when {
        store.isDownloaded(videoId) -> DownloadStatus.Completed
        else -> active[videoId] ?: DownloadStatus.None
    }

    fun download(song: Song) {
        val id = song.videoId
        if (store.isDownloaded(id)) return
        val cur = active[id]
        if (cur is DownloadStatus.Queued || cur is DownloadStatus.Downloading) return
        setStatus(id, DownloadStatus.Queued)
        pool.submit { runDownload(song) }
    }

    private fun runDownload(song: Song) {
        val id = song.videoId
        try {
            val ytdlp = YtDlp.path ?: throw IllegalStateException("yt-dlp not found. Install: brew install yt-dlp")
            setStatus(id, DownloadStatus.Downloading(0))

            val outTemplate = java.io.File(store.dir, "$id.%(ext)s").absolutePath
            val proc = ProcessBuilder(
                ytdlp, "-f", "bestaudio/best", "--no-playlist", "--no-part", "--newline",
                "-o", outTemplate, "https://www.youtube.com/watch?v=$id",
            ).redirectErrorStream(true).start()

            proc.inputStream.bufferedReader().forEachLine { line ->
                progressPattern.find(line)?.let { m ->
                    setStatus(id, DownloadStatus.Downloading(m.groupValues[1].toFloat().toInt()))
                }
            }
            if (proc.waitFor() != 0) throw RuntimeException("yt-dlp failed")

            val audio = store.dir.listFiles { f ->
                f.name.startsWith("$id.") && !f.name.endsWith(".lrc") && !f.name.endsWith(".part")
            }?.firstOrNull() ?: throw RuntimeException("downloaded file not found")

            // Save lyrics (word-synced when available) for offline use.
            val lyricsJsonPath = saveLyrics(id, song)
            // Save the cover image too, so artwork shows offline.
            val coverPath = saveCover(id, song.thumbnailUrl)

            store.add(
                DownloadedSong(
                    videoId = id, title = song.title, artists = song.artists,
                    durationSeconds = song.durationSeconds, thumbnailUrl = song.thumbnailUrl,
                    audioPath = audio.absolutePath, coverPath = coverPath, lyricsJsonPath = lyricsJsonPath,
                ),
            )
            active.remove(id)
            notifyChanged(id, DownloadStatus.Completed)
            notifyLibrary()
        } catch (e: Exception) {
            setStatus(id, DownloadStatus.Failed(e.message ?: "download failed"))
        }
    }

    private fun saveCover(id: String, thumbnailUrl: String?): String? = runCatching {
        if (thumbnailUrl.isNullOrBlank()) return null
        val sized = Regex("w\\d+-h\\d+").replace(thumbnailUrl, "w544-h544")
        val f = java.io.File(store.dir, "$id.jpg")
        val conn = java.net.URI(sized).toURL().openConnection().apply {
            setRequestProperty("User-Agent", "Mozilla/5.0"); connectTimeout = 8000; readTimeout = 8000
        }
        conn.getInputStream().use { input -> f.outputStream().use { input.copyTo(it) } }
        f.absolutePath
    }.getOrNull()

    private fun saveLyrics(id: String, song: Song): String? = runCatching {
        val state = onlineLyrics.fetch(song)
        if (state is LyricsState.Loaded) {
            val f = store.lyricsJsonFileFor(id); f.writeText(json.encodeToString(state.toSaved())); f.absolutePath
        } else null
    }.getOrNull()

    /** Fetch missing (word-synced) lyrics for already-downloaded songs when online. */
    fun backfillLyrics() {
        pool.submit {
            var changed = false
            store.all().forEach { d ->
                val ok = d.lyricsJsonPath?.let { java.io.File(it).exists() } == true
                if (!ok) saveLyrics(d.videoId, d.toSong())?.let { p ->
                    store.add(d.copy(lyricsJsonPath = p)); changed = true
                }
            }
            if (changed) notifyLibrary()
        }
    }

    /** Fetch missing cover images for already-downloaded songs (self-heals older downloads when online). */
    fun backfillCovers() {
        pool.submit {
            var changed = false
            store.all().forEach { d ->
                val ok = d.coverPath?.let { java.io.File(it).exists() } == true
                if (!ok) saveCover(d.videoId, d.thumbnailUrl)?.let { cp ->
                    store.add(d.copy(coverPath = cp)); changed = true
                }
            }
            if (changed) notifyLibrary()
        }
    }

    private fun setStatus(id: String, status: DownloadStatus) {
        active[id] = status
        notifyChanged(id, status)
    }

    private fun notifyChanged(id: String, status: DownloadStatus) {
        SwingUtilities.invokeLater { listeners.forEach { it.onDownloadChanged(id, status) } }
    }

    private fun notifyLibrary() {
        SwingUtilities.invokeLater { listeners.forEach { it.onLibraryChanged() } }
    }
}
