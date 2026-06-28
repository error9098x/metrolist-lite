package com.metrolist.desktop.model

import com.metrolist.innertube.models.SongItem

/**
 * UI-facing song model, decoupled from innertube's [SongItem] (Dependency Inversion: the UI and
 * player depend on this small type, not on the large innertube parsing models).
 */
data class Song(
    val videoId: String,
    val title: String,
    val artists: String,
    val durationSeconds: Int?,
    val thumbnailUrl: String? = null,
) {
    val durationText: String
        get() = durationSeconds?.let { s ->
            "%d:%02d".format(s / 60, s % 60)
        } ?: "--:--"

    /** Single-line label used by the results list. */
    val displayLine: String
        get() = buildString {
            append(title)
            if (artists.isNotBlank()) append("  —  ").append(artists)
            append("   (").append(durationText).append(')')
        }

    companion object {
        fun from(item: SongItem): Song = Song(
            videoId = item.id,
            title = item.title,
            artists = item.artists.joinToString(", ") { it.name },
            durationSeconds = item.duration,
            thumbnailUrl = item.thumbnail,
        )
    }
}
