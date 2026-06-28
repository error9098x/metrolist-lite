package com.metrolist.desktop.lyrics

import com.metrolist.lrclib.LrcLib

/** Parses raw LRC / plain lyric text into a [LyricsState]. Used for both network and offline lyrics. */
object LyricsParser {
    private val bracket = Regex("""^\s*\[[^\]]*\]\s*""")

    fun parse(text: String?): LyricsState {
        if (text.isNullOrBlank()) return LyricsState.Unavailable

        val sentences = LrcLib.Lyrics(text).sentences
        if (sentences != null && sentences.size > 1) {
            val lines = sentences.entries
                .sortedBy { it.key }
                .map { LyricLine(it.key, it.value.trim()) }
                .filter { it.text.isNotEmpty() || it.timeMs == 0L }
            if (lines.any { it.timeMs > 0 }) return LyricsState.Loaded(lines, synced = true)
        }

        val plain = text.lines()
            .map { it.replaceFirst(bracket, "").trim() }
            .filter { it.isNotEmpty() }
            .map { LyricLine(-1, it) }
        return if (plain.isEmpty()) LyricsState.Unavailable else LyricsState.Loaded(plain, synced = false)
    }
}
