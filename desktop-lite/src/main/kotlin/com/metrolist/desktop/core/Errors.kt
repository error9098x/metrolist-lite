package com.metrolist.desktop.core

/**
 * Typed, user-facing errors. Every failure path in the app surfaces one of these with a clear
 * message so the UI can show something actionable and logs stay diagnosable ("self-healing":
 * the cause chain is always preserved).
 */
sealed class MetrolistException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The search box was empty/blank. */
class BlankQueryException : MetrolistException("Type something to search.")

/** The YouTube Music search request itself failed (network / parsing / rate-limit). */
class SearchFailedException(query: String, cause: Throwable?) :
    MetrolistException("Search failed for \"$query\": ${cause?.message ?: "unknown error"}", cause)

/** Could not turn a videoId into a playable audio URL via any resolver. */
class StreamResolutionException(videoId: String, reason: String, cause: Throwable? = null) :
    MetrolistException("No playable audio for $videoId: $reason", cause)

/** mpv is not installed / not on PATH. */
class MpvNotFoundException :
    MetrolistException("mpv not found. Install it with:  brew install mpv")

/** mpv failed to start or died immediately. */
class PlaybackException(reason: String, cause: Throwable? = null) :
    MetrolistException("Playback failed: $reason", cause)
