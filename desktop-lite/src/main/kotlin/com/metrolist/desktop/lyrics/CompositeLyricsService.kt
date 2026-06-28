package com.metrolist.desktop.lyrics

import com.metrolist.desktop.model.Song

/** Tries providers in order and returns the first that yields lyrics (word-synced preferred). */
class CompositeLyricsService(private val providers: List<LyricsService>) : LyricsService {
    override fun fetch(song: Song): LyricsState {
        providers.forEach { p ->
            val s = runCatching { p.fetch(song) }.getOrNull()
            if (s is LyricsState.Loaded) return s
        }
        return LyricsState.Unavailable
    }
}
