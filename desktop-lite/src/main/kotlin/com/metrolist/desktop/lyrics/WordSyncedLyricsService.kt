package com.metrolist.desktop.lyrics

import com.metrolist.desktop.model.Song
import com.metrolist.music.betterlyrics.TTMLParser
import com.metrolist.music.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Word-by-word synced lyrics from the BetterLyrics TTML API (reused `betterlyrics` module). Returns
 * [LyricsState.Loaded] with per-word timings; null/Unavailable when no word-synced lyrics exist.
 */
class WordSyncedLyricsService : LyricsService {

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 10000; socketTimeoutMillis = 15000 }
            defaultRequest {
                url("https://lyrics-api.boidu.dev")
                headers { append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36") }
            }
            expectSuccess = false
        }
    }

    override fun fetch(song: Song): LyricsState {
        if (song.title.isBlank()) return LyricsState.Unavailable
        val ttml = runCatching {
            runBlocking {
                val resp = client.get("/getLyrics") {
                    parameter("s", song.title)
                    parameter("a", song.artists)
                    (song.durationSeconds ?: -1).let { if (it > 0) parameter("d", it) }
                }
                if (resp.status == HttpStatusCode.OK) resp.body<TTMLResponse>().ttml else null
            }
        }.getOrNull()
        if (ttml.isNullOrBlank()) return LyricsState.Unavailable

        val parsed = runCatching { TTMLParser.parseTTML(ttml) }.getOrNull().orEmpty()
        val lines = parsed
            .filter { !it.isBackground && it.text.isNotBlank() }
            .map { pl ->
                LyricLine(
                    timeMs = (pl.startTime * 1000).toLong(),
                    text = pl.text,
                    words = pl.words.map { w -> WordTiming(w.text, (w.startTime * 1000).toLong(), (w.endTime * 1000).toLong()) },
                )
            }
        return if (lines.isEmpty()) LyricsState.Unavailable else LyricsState.Loaded(lines, synced = true)
    }
}
