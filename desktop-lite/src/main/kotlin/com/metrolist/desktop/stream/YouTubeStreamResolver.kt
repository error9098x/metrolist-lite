package com.metrolist.desktop.stream

import com.metrolist.desktop.core.StreamResolutionException
import com.metrolist.desktop.download.DownloadStore
import com.metrolist.desktop.innertube.YouTubeBootstrap
import com.metrolist.desktop.player.YtDlp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.NewPipeUtils
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Two-strategy resolver (self-healing): try the most reliable path first, fall back to the second.
 *
 *  1. NewPipe `StreamInfo` — NewPipe picks a working client, applies signature / n-param /
 *     throttling deobfuscation, and returns a ready-to-stream audio URL. Most robust, avoids the
 *     WEB poToken problem entirely.
 *  2. innertube `YouTube.player(WEB_REMIX)` adaptiveFormats + `NewPipeUtils.getStreamUrl` — the
 *     path documented in CLAUDE.md, kept as a fallback.
 *
 * Referencing [NewPipeUtils] forces its initializer, which calls `NewPipe.init(...)` exactly once.
 */
class YouTubeStreamResolver(private val downloads: DownloadStore? = null) : StreamResolver {

    // Touching the object triggers `NewPipe.init(...)` (done in NewPipeUtils' initializer).
    private val newPipeInit = NewPipeUtils

    override fun resolve(videoId: String): String {
        require(videoId.isNotBlank()) { "videoId must not be blank" }
        // Keep the init reference observably used.
        checkNotNull(newPipeInit)

        // Offline first: play the downloaded local file if we have it.
        downloads?.audioPath(videoId)?.let { return it }

        // Preferred path: hand mpv the watch URL and let its yt-dlp resolve a poToken-valid audio
        // stream. This avoids the HTTP 403 that bare WEB/WEB_REMIX stream URLs now hit.
        if (YtDlp.isAvailable()) {
            return "https://www.youtube.com/watch?v=$videoId"
        }

        val errors = mutableListOf<Throwable>()

        resolveViaNewPipe(videoId).onSuccess { return it }.onFailure { errors += it }
        resolveViaInnertube(videoId).onSuccess { return it }.onFailure { errors += it }

        throw StreamResolutionException(
            videoId,
            reason = errors.joinToString(" | ") { it.message ?: it.javaClass.simpleName }
                .ifBlank { "all resolvers returned no audio" },
            cause = errors.firstOrNull(),
        )
    }

    private fun resolveViaNewPipe(videoId: String): Result<String> = runCatching {
        val info = StreamInfo.getInfo(
            NewPipe.getService(0), // 0 = YouTube
            "https://www.youtube.com/watch?v=$videoId",
        )
        info.audioStreams
            .filter { !it.content.isNullOrBlank() }
            .maxByOrNull { it.averageBitrate.takeIf { b -> b > 0 } ?: it.bitrate }
            ?.content
            ?: error("NewPipe returned no audio streams")
    }

    private fun resolveViaInnertube(videoId: String): Result<String> = runCatching {
        YouTubeBootstrap.ensure()
        val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()

        val player = runBlocking {
            YouTube.player(
                videoId = videoId,
                client = YouTubeClient.WEB_REMIX,
                signatureTimestamp = signatureTimestamp,
            )
        }.getOrThrow()

        val status = player.playabilityStatus.status
        if (!status.equals("OK", ignoreCase = true)) {
            error("playabilityStatus=$status ${player.playabilityStatus.reason.orEmpty()}".trim())
        }

        val bestAudio = player.streamingData
            ?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull { it.bitrate }
            ?: error("no audio adaptiveFormats")

        NewPipeUtils.getStreamUrl(bestAudio, videoId).getOrThrow()
    }
}
