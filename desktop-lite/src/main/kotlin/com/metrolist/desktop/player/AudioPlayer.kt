package com.metrolist.desktop.player

/**
 * Plays a single audio stream at a time. Starting a new track stops the previous one.
 * Implementations must keep audio running independent of the UI window state.
 */
interface AudioPlayer {
    /** Stop any current playback and start streaming [url]. @throws com.metrolist.desktop.core.MetrolistException */
    fun play(url: String)

    /** Stop playback if anything is playing. Safe to call when idle. */
    fun stop()

    fun pause()
    fun resume()
    /** Best-effort pause/resume toggle. */
    fun togglePause()

    /** Seek to an absolute position in seconds (best-effort). */
    fun seekTo(seconds: Double)

    /** Set output volume 0..100 (best-effort, persisted across tracks). */
    fun setVolume(percent: Int)

    /** True while a track is loaded (playing or paused). */
    val isPlaying: Boolean
    val isPaused: Boolean

    /** Current playback position / total duration in seconds, or null if unknown. */
    fun positionSeconds(): Double?
    fun durationSeconds(): Double?
}
