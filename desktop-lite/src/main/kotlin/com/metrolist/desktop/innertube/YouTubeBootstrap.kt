package com.metrolist.desktop.innertube

import com.metrolist.innertube.YouTube
import kotlinx.coroutines.runBlocking

/**
 * One-time, best-effort setup of the shared [YouTube] object before the first request.
 *
 * YouTube Music increasingly expects a `visitorData` token on anonymous requests. We fetch one
 * once and cache it on [YouTube]. Failure is non-fatal — search/playback are attempted anyway.
 */
object YouTubeBootstrap {
    @Volatile
    private var initialized = false

    @Synchronized
    fun ensure() {
        if (initialized) return
        runCatching {
            runBlocking { YouTube.visitorData() }.getOrNull()?.let { token ->
                YouTube.visitorData = token
            }
        }
        initialized = true
    }
}
