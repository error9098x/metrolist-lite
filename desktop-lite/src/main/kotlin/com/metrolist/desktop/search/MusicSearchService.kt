package com.metrolist.desktop.search

import com.metrolist.desktop.model.Song

/**
 * Searches a music catalog for songs. Implementations are blocking and throw
 * [com.metrolist.desktop.core.MetrolistException] subtypes on failure (Interface Segregation:
 * the UI only needs this one method).
 */
fun interface MusicSearchService {
    /** @throws com.metrolist.desktop.core.MetrolistException on blank query or request failure. */
    fun search(query: String): List<Song>
}
