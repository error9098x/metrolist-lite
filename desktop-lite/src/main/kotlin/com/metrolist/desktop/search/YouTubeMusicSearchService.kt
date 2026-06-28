package com.metrolist.desktop.search

import com.metrolist.desktop.core.BlankQueryException
import com.metrolist.desktop.core.SearchFailedException
import com.metrolist.desktop.innertube.YouTubeBootstrap
import com.metrolist.desktop.model.Song
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking

/**
 * [MusicSearchService] backed by the reused innertube `YouTube.search` API, filtered to songs.
 */
class YouTubeMusicSearchService : MusicSearchService {

    override fun search(query: String): List<Song> {
        val q = query.trim()
        if (q.isEmpty()) throw BlankQueryException()

        YouTubeBootstrap.ensure()

        val result = runCatching {
            runBlocking { YouTube.search(q, YouTube.SearchFilter.FILTER_SONG) }
        }.getOrElse { throw SearchFailedException(q, it) }

        val searchResult = result.getOrElse { throw SearchFailedException(q, it) }

        return searchResult.items
            .filterIsInstance<SongItem>()
            .map(Song::from)
    }
}
