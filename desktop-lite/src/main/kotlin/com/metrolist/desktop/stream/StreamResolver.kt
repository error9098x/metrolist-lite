package com.metrolist.desktop.stream

/**
 * Resolves a YouTube videoId into a directly-playable audio stream URL.
 */
fun interface StreamResolver {
    /** @throws com.metrolist.desktop.core.StreamResolutionException when no audio URL can be produced. */
    fun resolve(videoId: String): String
}
