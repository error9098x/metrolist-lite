package com.metrolist.desktop.playback

import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.model.Song

/** Observer of [PlaybackController]. All callbacks are delivered on the Swing EDT. */
interface PlaybackListener {
    fun onSearchResults(query: String, songs: List<Song>) {}
    fun onQueue(songs: List<Song>, index: Int) {}
    fun onTrackChanged(song: Song?, index: Int) {}
    fun onPlaybackState(playing: Boolean, paused: Boolean) {}
    fun onProgress(positionSec: Double, durationSec: Double) {}
    fun onModes(shuffle: Boolean, repeat: RepeatMode) {}
    fun onLyrics(state: LyricsState) {}
    fun onStatus(message: String) {}
}

enum class RepeatMode { NONE, ALL, ONE }
