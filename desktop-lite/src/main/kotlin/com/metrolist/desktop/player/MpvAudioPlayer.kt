package com.metrolist.desktop.player

import com.metrolist.desktop.core.MpvNotFoundException
import com.metrolist.desktop.core.PlaybackException
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * [AudioPlayer] that drives the external `mpv` binary (one process per track).
 *
 * Network audio plays via mpv's bundled yt-dlp on a YouTube watch URL (poToken-valid). Because mpv
 * is a separate process, playback survives window minimize/blur. Transport (pause/seek/volume/
 * position) is driven over mpv's JSON IPC on a unix-domain socket (Java 16+).
 */
class MpvAudioPlayer : AudioPlayer {

    @Volatile private var process: Process? = null
    @Volatile private var ipcSocketPath: String? = null
    @Volatile private var paused = false
    @Volatile private var volume = 100
    private val reqId = AtomicInteger(0)

    override val isPlaying: Boolean get() = process?.isAlive == true
    override val isPaused: Boolean get() = paused

    @Synchronized
    override fun play(url: String) {
        require(url.isNotBlank()) { "stream url must not be blank" }
        stop()

        val mpv = resolveMpvPath() ?: throw MpvNotFoundException()

        val socketPath = File(
            System.getProperty("java.io.tmpdir"),
            "metrolist-mpv-${System.nanoTime()}.sock",
        ).absolutePath
        val logFile = File.createTempFile("metrolist-mpv-", ".log").apply { deleteOnExit() }

        val command = buildList {
            add(mpv)
            add("--no-video")
            add("--no-terminal")
            add("--really-quiet")
            add("--no-config")
            add("--idle=no")
            add("--volume=$volume")
            add("--ytdl-format=bestaudio/best")
            YtDlp.path?.let { add("--script-opts=ytdl_hook-ytdl_path=$it") }
            add("--input-ipc-server=$socketPath")
            add(url)
        }

        val proc = try {
            ProcessBuilder(command).redirectErrorStream(true).redirectOutput(logFile).start()
        } catch (t: Throwable) {
            throw PlaybackException("could not start mpv ($mpv)", t)
        }

        if (proc.waitFor(350, TimeUnit.MILLISECONDS)) {
            val tail = runCatching { logFile.readText().trim().takeLast(400) }.getOrDefault("")
            throw PlaybackException("mpv exited (code ${proc.exitValue()})${if (tail.isNotBlank()) ": $tail" else ""}")
        }

        process = proc
        ipcSocketPath = socketPath
        paused = false
    }

    @Synchronized
    override fun stop() {
        process?.let { p ->
            p.destroy()
            if (!p.waitFor(500, TimeUnit.MILLISECONDS)) p.destroyForcibly()
        }
        process = null
        ipcSocketPath?.let { runCatching { File(it).delete() } }
        ipcSocketPath = null
        paused = false
    }

    override fun pause() { setPaused(true) }
    override fun resume() { setPaused(false) }

    override fun togglePause() {
        if (!isPlaying) return
        send("""{"command":["cycle","pause"]}""", readReply = false)
        paused = !paused
    }

    private fun setPaused(value: Boolean) {
        if (!isPlaying) return
        send("""{"command":["set_property","pause",$value]}""", readReply = false)
        paused = value
    }

    override fun seekTo(seconds: Double) {
        if (!isPlaying) return
        send("""{"command":["seek",$seconds,"absolute"]}""", readReply = false)
    }

    override fun setVolume(percent: Int) {
        volume = percent.coerceIn(0, 100)
        if (isPlaying) send("""{"command":["set_property","volume",$volume]}""", readReply = false)
    }

    override fun positionSeconds(): Double? = queryDouble("time-pos")
    override fun durationSeconds(): Double? = queryDouble("duration")

    // ---- mpv IPC ----

    private fun send(commandJson: String, readReply: Boolean): String? {
        val socket = ipcSocketPath ?: return null
        if (process?.isAlive != true) return null
        return runCatching {
            SocketChannel.open(StandardProtocolFamily.UNIX).use { ch ->
                ch.connect(UnixDomainSocketAddress.of(socket))
                ch.write(ByteBuffer.wrap((commandJson + "\n").toByteArray(StandardCharsets.UTF_8)))
                if (!readReply) return@use null
                // Non-blocking read with a hard timeout so an unresponsive mpv never hangs the caller.
                ch.configureBlocking(false)
                java.nio.channels.Selector.open().use { sel ->
                    ch.register(sel, java.nio.channels.SelectionKey.OP_READ)
                    if (sel.select(400) == 0) return@use null
                    val buf = ByteBuffer.allocate(16384)
                    val n = ch.read(buf)
                    if (n <= 0) null else String(buf.array(), 0, n, StandardCharsets.UTF_8)
                }
            }
        }.getOrNull()
    }

    private fun queryDouble(property: String): Double? {
        val id = reqId.incrementAndGet()
        val resp = send("""{"command":["get_property","$property"],"request_id":$id}""", readReply = true) ?: return null
        val line = resp.lineSequence().firstOrNull { it.contains("\"request_id\":$id") }
            ?: resp.lineSequence().firstOrNull { it.contains("\"data\"") }
            ?: return null
        return Regex("\"data\":\\s*(-?[0-9]+(?:\\.[0-9]+)?)").find(line)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun resolveMpvPath(): String? {
        BundledBinaries.path("mpv")?.let { return it } // bundled in the packaged .app
        System.getenv("METROLIST_MPV")?.let { if (File(it).canExecute()) return it }
        runCatching {
            val which = ProcessBuilder("/bin/sh", "-c", "command -v mpv").start()
            val out = which.inputStream.bufferedReader().readText().trim()
            which.waitFor(2, TimeUnit.SECONDS)
            if (out.isNotBlank() && File(out).canExecute()) return out
        }
        return listOf("/opt/homebrew/bin/mpv", "/usr/local/bin/mpv", "/usr/bin/mpv")
            .firstOrNull { File(it).canExecute() }
    }
}
