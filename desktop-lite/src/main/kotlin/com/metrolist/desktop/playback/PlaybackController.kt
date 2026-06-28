package com.metrolist.desktop.playback

import com.metrolist.desktop.core.MetrolistException
import com.metrolist.desktop.lyrics.LyricsService
import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.model.Song
import com.metrolist.desktop.player.AudioPlayer
import com.metrolist.desktop.search.MusicSearchService
import com.metrolist.desktop.stream.StreamResolver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.random.Random

/**
 * Single source of truth for playback: holds the queue, current index, shuffle/repeat modes, and
 * orchestrates search → resolve → play on background threads. A poller drives progress updates and
 * auto-advances when a track ends. UI observes via [PlaybackListener] (callbacks on the EDT).
 */
class PlaybackController(
    private val search: MusicSearchService,
    private val resolver: StreamResolver,
    private val player: AudioPlayer,
    private val lyrics: LyricsService,
) {
    private val listeners = CopyOnWriteArrayList<PlaybackListener>()
    private val work = Executors.newSingleThreadExecutor { r -> Thread(r, "playback").apply { isDaemon = true } }
    private val searchExec = Executors.newSingleThreadExecutor { r -> Thread(r, "search").apply { isDaemon = true } }
    private val lyricsExec = Executors.newSingleThreadExecutor { r -> Thread(r, "lyrics").apply { isDaemon = true } }
    private val ticker = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "playback-tick").apply { isDaemon = true } }

    @Volatile private var lastLyrics: LyricsState = LyricsState.Unavailable
    @Volatile private var playGen = 0

    private val queue = mutableListOf<Song>()
    @Volatile private var index = -1
    @Volatile private var shuffle = false
    @Volatile private var repeat = RepeatMode.NONE

    @Volatile private var resolving = false
    @Volatile private var active = false      // a track is meant to be playing (for end-detection)
    @Volatile private var lastAlive = false

    init {
        ticker.scheduleWithFixedDelay({ tick() }, 500, 500, TimeUnit.MILLISECONDS)
    }

    fun addListener(l: PlaybackListener) {
        listeners += l
        // Push current snapshot to the new listener.
        edt {
            l.onModes(shuffle, repeat)
            current()?.let { l.onTrackChanged(it, index) }
            l.onPlaybackState(player.isPlaying, player.isPaused)
            l.onLyrics(lastLyrics)
        }
    }

    fun current(): Song? = queue.getOrNull(index)

    // ---- search ----

    fun search(query: String) {
        searchExec.submit {
            try {
                val results = search.search(query)
                edt {
                    listeners.forEach { it.onSearchResults(query, results) }
                    listeners.forEach { it.onStatus(if (results.isEmpty()) "No songs found." else "${results.size} results.") }
                }
            } catch (e: MetrolistException) {
                edt { listeners.forEach { it.onStatus(e.message ?: "Search failed.") } }
            } catch (e: Exception) {
                edt { listeners.forEach { it.onStatus("Unexpected error: ${e.message}") } }
            }
        }
    }

    // ---- queue control ----

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        synchronized(queue) {
            queue.clear(); queue.addAll(songs)
            index = startIndex.coerceIn(0, songs.lastIndex)
        }
        edt { listeners.forEach { it.onQueue(songs, index) } }
        playCurrent()
    }

    fun next() {
        val n = nextIndex(forward = true, userTriggered = true) ?: return
        index = n; playCurrent()
    }

    fun previous() {
        // Restart current if more than 3s in, else go back.
        val pos = player.positionSeconds() ?: 0.0
        if (pos > 3.0) { player.seekTo(0.0); return }
        val p = nextIndex(forward = false, userTriggered = true) ?: return
        index = p; playCurrent()
    }

    fun togglePause() {
        player.togglePause()
        edt { listeners.forEach { it.onPlaybackState(player.isPlaying, player.isPaused) } }
    }

    /** Main play/pause button: pause/resume if a track is loaded, else (re)start the current one. */
    fun togglePlay() {
        if (player.isPlaying) togglePause() else current()?.let { playCurrent() }
    }

    fun stop() {
        active = false
        player.stop()
        edt { listeners.forEach { it.onPlaybackState(false, false) } }
    }

    fun seekTo(seconds: Double) = player.seekTo(seconds)
    fun setVolume(percent: Int) = player.setVolume(percent)

    fun toggleShuffle() {
        shuffle = !shuffle
        edt { listeners.forEach { it.onModes(shuffle, repeat) } }
    }

    fun cycleRepeat() {
        repeat = when (repeat) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        edt { listeners.forEach { it.onModes(shuffle, repeat) } }
    }

    // ---- internals ----

    private fun playCurrent() {
        val song = current() ?: return
        val gen = ++playGen
        active = true
        resolving = true
        edt {
            listeners.forEach { it.onTrackChanged(song, index) }
            listeners.forEach { it.onStatus("Resolving: ${song.title}…") }
        }
        fetchLyrics(song)
        work.submit {
            // A newer play request superseded this one — drop it.
            if (gen != playGen) { resolving = false; return@submit }
            try {
                val url = resolver.resolve(song.videoId)
                if (gen != playGen) return@submit
                player.play(url)
                lastAlive = true
                edt {
                    if (gen != playGen) return@edt
                    listeners.forEach { it.onPlaybackState(true, false) }
                    listeners.forEach { it.onStatus("Playing: ${song.title}") }
                }
            } catch (e: MetrolistException) {
                if (gen == playGen) active = false
                edt { listeners.forEach { it.onStatus(e.message ?: "Playback failed.") } }
            } catch (e: Exception) {
                if (gen == playGen) active = false
                edt { listeners.forEach { it.onStatus("Unexpected error: ${e.message}") } }
            } finally {
                if (gen == playGen) resolving = false
            }
        }
    }

    private fun fetchLyrics(song: Song) {
        lastLyrics = LyricsState.Loading
        edt { listeners.forEach { it.onLyrics(LyricsState.Loading) } }
        lyricsExec.submit {
            val state = lyrics.fetch(song)
            if (current()?.videoId == song.videoId) {
                lastLyrics = state
                edt { listeners.forEach { it.onLyrics(state) } }
            }
        }
    }

    private fun nextIndex(forward: Boolean, userTriggered: Boolean): Int? {
        if (queue.isEmpty()) return null
        if (repeat == RepeatMode.ONE && !userTriggered) return index
        if (shuffle) {
            if (queue.size == 1) return index
            var r: Int
            do { r = Random.nextInt(queue.size) } while (r == index)
            return r
        }
        val n = if (forward) index + 1 else index - 1
        return when {
            n in queue.indices -> n
            repeat == RepeatMode.ALL -> if (forward) 0 else queue.lastIndex
            else -> if (userTriggered) n.coerceIn(0, queue.lastIndex) else null // null => stop at end
        }
    }

    /** Periodic: emit progress and detect track-end to auto-advance. */
    private fun tick() {
        val playing = player.isPlaying
        if (active && !resolving) {
            if (lastAlive && !playing) {
                // Track ended naturally.
                lastAlive = false
                onTrackEnded()
            } else if (playing) {
                lastAlive = true
                val pos = player.positionSeconds() ?: return
                val dur = player.durationSeconds() ?: 0.0
                edt { listeners.forEach { it.onProgress(pos, dur) } }
            }
        }
    }

    private fun onTrackEnded() {
        if (repeat == RepeatMode.ONE) { playCurrent(); return }
        val n = nextIndex(forward = true, userTriggered = false)
        if (n == null) {
            active = false
            edt { listeners.forEach { it.onPlaybackState(false, false) } }
        } else {
            index = n; playCurrent()
        }
    }

    private inline fun edt(crossinline block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater { block() }
    }
}
