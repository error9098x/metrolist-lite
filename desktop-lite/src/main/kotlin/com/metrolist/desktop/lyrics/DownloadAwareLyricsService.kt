package com.metrolist.desktop.lyrics

import com.metrolist.desktop.download.DownloadStore
import com.metrolist.desktop.model.Song

/** Uses saved offline lyrics when the track is downloaded; otherwise falls back to the network. */
class DownloadAwareLyricsService(
    private val store: DownloadStore,
    private val online: LyricsService,
) : LyricsService {
    override fun fetch(song: Song): LyricsState {
        store.lyricsState(song.videoId)?.let { return it }          // offline word-synced JSON
        store.lrcText(song.videoId)?.let { return LyricsParser.parse(it) } // legacy .lrc (line-synced)
        return online.fetch(song)
    }
}
