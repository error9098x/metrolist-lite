package com.metrolist.desktop.lyrics

import com.metrolist.desktop.model.Song
import com.metrolist.lrclib.LrcLib
import kotlinx.coroutines.runBlocking

/** [LyricsService] backed by the reused `lrclib` module (LRCLIB.net). */
class LrcLibLyricsService : LyricsService {
    override fun fetch(song: Song): LyricsState {
        if (song.title.isBlank()) return LyricsState.Unavailable
        val text = runCatching {
            runBlocking { LrcLib.getLyrics(song.title, song.artists, song.durationSeconds ?: -1) }.getOrNull()
        }.getOrNull()
        return LyricsParser.parse(text)
    }
}
