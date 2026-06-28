package com.metrolist.desktop

import com.metrolist.desktop.playback.PlaybackController
import com.metrolist.desktop.player.MpvAudioPlayer
import com.metrolist.desktop.search.YouTubeMusicSearchService
import com.metrolist.desktop.stream.YouTubeStreamResolver
import com.metrolist.desktop.ui.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Composition root: build the concrete services and show the window on the EDT.
 */
fun main() {
    // Headless self-check of the whole non-UI pipeline (search -> resolve -> mpv detection).
    // Enabled with METROLIST_SMOKE=1 so the app can be validated without opening a window.
    if (System.getenv("METROLIST_SMOKE") != null) {
        runSmoke()
        return
    }

    // Native-feeling macOS menu bar + dark window chrome.
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.appearance", "system")
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

    // macOS Dock icon.
    runCatching {
        if (java.awt.Taskbar.isTaskbarSupported()) {
            com.metrolist.desktop.ui.AppIcon.image?.let { java.awt.Taskbar.getTaskbar().iconImage = it }
        }
    }

    val store = com.metrolist.desktop.download.DownloadStore()
    val onlineLyrics = com.metrolist.desktop.lyrics.CompositeLyricsService(
        listOf(com.metrolist.desktop.lyrics.WordSyncedLyricsService(), com.metrolist.desktop.lyrics.LrcLibLyricsService()),
    )
    val downloads = com.metrolist.desktop.download.DownloadManager(store, onlineLyrics)
    val lyrics = com.metrolist.desktop.lyrics.DownloadAwareLyricsService(store, onlineLyrics)

    val controller = PlaybackController(
        search = YouTubeMusicSearchService(),
        resolver = YouTubeStreamResolver(store),
        player = MpvAudioPlayer(),
        lyrics = lyrics,
    )

    SwingUtilities.invokeLater {
        MainWindow(controller, store, downloads).isVisible = true
    }
}

/** Exercises search + stream resolution + mpv detection end-to-end, printing a report. */
private fun runSmoke() {
    if (System.getenv("METROLIST_SMOKE_OFFLINE") != null) {
        val store = com.metrolist.desktop.download.DownloadStore()
        val d = store.all().firstOrNull()
        if (d == null) { println("[offline] no downloads to test"); return }
        println("[offline] track: ${d.title}")
        val url = com.metrolist.desktop.stream.YouTubeStreamResolver(store).resolve(d.videoId)
        println("[offline] resolved=${url}  isLocalFile=${java.io.File(url).exists()}")
        val player = MpvAudioPlayer()
        player.play(url); Thread.sleep(4000)
        println("[offline] playing=${player.isPlaying}"); player.stop()
        val lyr = com.metrolist.desktop.lyrics.DownloadAwareLyricsService(store, com.metrolist.desktop.lyrics.LrcLibLyricsService()).fetch(d.toSong())
        println("[offline] lyrics=${if (lyr is com.metrolist.desktop.lyrics.LyricsState.Loaded) "Loaded ${lyr.lines.size} synced=${lyr.synced}" else lyr}")
        println("[offline] DONE")
        return
    }
    val query = System.getenv("METROLIST_SMOKE_QUERY") ?: "daft punk get lucky"
    println("[smoke] query: \"$query\"")
    try {
        val results = YouTubeMusicSearchService().search(query)
        println("[smoke] results: ${results.size}")
        results.take(5).forEachIndexed { i, s -> println("  ${i + 1}. ${s.displayLine}  [${s.videoId}]") }

        val first = results.firstOrNull() ?: run { println("[smoke] no results — aborting"); return }
        println("[smoke] resolving stream for: ${first.title}")
        val url = YouTubeStreamResolver().resolve(first.videoId)
        println("[smoke] stream URL ok (len=${url.length}): ${url.take(80)}…")
        java.io.File("/tmp/ml-url.txt").writeText(url)
        println("[smoke] full URL written to /tmp/ml-url.txt")

        val player = MpvAudioPlayer()
        val playReport = runCatching {
            player.play(url)
            // Hold long enough for yt-dlp to resolve and mpv to actually open the audio device.
            Thread.sleep(6000)
            val alive = player.isPlaying
            player.stop()
            if (alive) "mpv playing OK" else "mpv started but exited early"
        }.getOrElse { it.message ?: it.javaClass.simpleName }
        println("[smoke] playback: $playReport")

        val lyrics = com.metrolist.desktop.lyrics.CompositeLyricsService(
            listOf(com.metrolist.desktop.lyrics.WordSyncedLyricsService(), com.metrolist.desktop.lyrics.LrcLibLyricsService()),
        ).fetch(first)
        when (lyrics) {
            is com.metrolist.desktop.lyrics.LyricsState.Loaded -> {
                val wl = lyrics.lines.firstOrNull { it.words.isNotEmpty() }
                println("[smoke] lyrics: ${lyrics.lines.size} lines, synced=${lyrics.synced}, wordSynced=${lyrics.lines.any { it.words.isNotEmpty() }}")
                if (wl != null) println("[smoke] words: " + wl.words.take(6).joinToString(" ") { "${it.text}@${it.startMs}ms" })
            }
            else -> println("[smoke] lyrics: $lyrics")
        }
        if (System.getenv("METROLIST_SMOKE_DL") != null) {
            val store = com.metrolist.desktop.download.DownloadStore()
            val dm = com.metrolist.desktop.download.DownloadManager(
                store,
                com.metrolist.desktop.lyrics.CompositeLyricsService(
                    listOf(com.metrolist.desktop.lyrics.WordSyncedLyricsService(), com.metrolist.desktop.lyrics.LrcLibLyricsService()),
                ),
            )
            println("[smoke] downloading ${first.title}…")
            dm.download(first)
            val t0 = System.currentTimeMillis()
            while (!store.isDownloaded(first.videoId) && System.currentTimeMillis() - t0 < 120_000) {
                Thread.sleep(1500)
                val st = dm.statusOf(first.videoId)
                if (st is com.metrolist.desktop.download.DownloadStatus.Failed) { println("[smoke] download FAILED: ${st.message}"); break }
            }
            val d = store.get(first.videoId)
            println("[smoke] downloaded=${d != null} audio=${d?.audioPath} lrc=${d?.lrcPath}")
        }
        println("[smoke] DONE")
    } catch (e: Exception) {
        println("[smoke] FAILED: ${e.message}")
        e.printStackTrace()
    }
}
