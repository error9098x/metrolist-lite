package com.metrolist.desktop.download

import com.metrolist.desktop.model.Song
import kotlinx.serialization.Serializable

/** A song saved to disk for offline playback (audio + optional synced .lrc). */
@Serializable
data class DownloadedSong(
    val videoId: String,
    val title: String,
    val artists: String,
    val durationSeconds: Int? = null,
    val thumbnailUrl: String? = null,
    val audioPath: String,
    val lrcPath: String? = null,
    val coverPath: String? = null,
    val lyricsJsonPath: String? = null,
) {
    // Prefer the locally-saved cover so artwork shows offline.
    fun toSong() = Song(videoId, title, artists, durationSeconds, coverPath ?: thumbnailUrl)
}

/** Live status of a download. */
sealed interface DownloadStatus {
    data object None : DownloadStatus
    data object Queued : DownloadStatus
    data class Downloading(val percent: Int) : DownloadStatus
    data object Completed : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

interface DownloadListener {
    /** A single track's status changed. */
    fun onDownloadChanged(videoId: String, status: DownloadStatus) {}
    /** The set of completed downloads changed (added/removed). */
    fun onLibraryChanged() {}
}
