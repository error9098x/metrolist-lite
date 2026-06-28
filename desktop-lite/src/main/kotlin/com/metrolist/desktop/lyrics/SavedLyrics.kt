package com.metrolist.desktop.lyrics

import kotlinx.serialization.Serializable

/** On-disk representation of resolved lyrics (keeps per-word timings for offline word-by-word). */
@Serializable
data class SavedLyrics(val synced: Boolean, val lines: List<SavedLine>)

@Serializable
data class SavedLine(val ms: Long, val text: String, val words: List<SavedWord> = emptyList())

@Serializable
data class SavedWord(val t: String, val s: Long, val e: Long)

fun LyricsState.Loaded.toSaved(): SavedLyrics =
    SavedLyrics(synced, lines.map { l -> SavedLine(l.timeMs, l.text, l.words.map { SavedWord(it.text, it.startMs, it.endMs) }) })

fun SavedLyrics.toState(): LyricsState =
    if (lines.isEmpty()) LyricsState.Unavailable
    else LyricsState.Loaded(lines.map { l -> LyricLine(l.ms, l.text, l.words.map { WordTiming(it.t, it.s, it.e) }) }, synced)
