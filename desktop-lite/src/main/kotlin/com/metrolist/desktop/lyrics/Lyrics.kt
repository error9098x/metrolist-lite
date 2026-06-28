package com.metrolist.desktop.lyrics

import com.metrolist.desktop.model.Song

/** A single word with its sung start/end time (ms), for word-by-word highlighting. */
data class WordTiming(val text: String, val startMs: Long, val endMs: Long)

/**
 * One lyric line. [timeMs] is the synced start time in ms, or -1 for plain (untimed) lyrics.
 * [words] carries per-word timings when the source provides them (else empty).
 */
data class LyricLine(val timeMs: Long, val text: String, val words: List<WordTiming> = emptyList())

/** State of a lyrics lookup for the current track. */
sealed interface LyricsState {
    data object Loading : LyricsState
    data object Unavailable : LyricsState
    data class Loaded(val lines: List<LyricLine>, val synced: Boolean) : LyricsState
}

/** Fetches lyrics for a track (blocking; returns a state, never throws). */
fun interface LyricsService {
    fun fetch(song: Song): LyricsState
}
