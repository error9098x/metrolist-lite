package com.metrolist.desktop.ui

import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.model.Song
import com.metrolist.desktop.ui.components.CoverView
import com.metrolist.desktop.ui.components.IconButton
import com.metrolist.desktop.ui.components.SeekBar
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Apple-Music-style Now Playing: album art + progress on the left, large flowing lyrics filling the
 * right, over an album-art gradient. Transport lives in the persistent bottom bar, so there are no
 * controls here.
 */
class NowPlayingPanel(
    private val controls: Controls,
    private val onToggleFullscreen: () -> Unit = {},
) : JPanel() {

    private val cover = CoverView(380, 24)
    private val title = JLabel("Nothing playing")
    private val artist = JLabel("")
    private val status = JLabel("Stopped")
    private val seek = SeekBar(knobRadius = 7)
    private val posLabel = JLabel("0:00")
    private val durLabel = JLabel("0:00")
    private val lyrics = LyricsView(compact = false, paintBackground = false)

    private var durationSec = 0.0
    private var grad1: Color = Theme.surface
    private var grad2: Color = Theme.bg

    init {
        background = Theme.bg
        isOpaque = true
        layout = BorderLayout(48, 0)
        border = BorderFactory.createEmptyBorder(44, 56, 44, 40)

        add(leftColumn(), BorderLayout.WEST)
        add(lyrics, BorderLayout.CENTER)
        add(topBar(), BorderLayout.NORTH)

        seek.onSeek = { f -> if (durationSec > 0) controls.seek(f * durationSec) }
    }

    private fun topBar(): JPanel {
        val fullscreen = IconButton("expand", box = 18, pad = 9)
        fullscreen.onClick = { onToggleFullscreen() }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
            add(fullscreen, BorderLayout.EAST)
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.paint = GradientPaint(0f, 0f, grad1, 0f, height.toFloat(), grad2)
        g2.fillRect(0, 0, width, height)
        g2.dispose()
    }

    private fun leftColumn(): JPanel {
        val content = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS) }

        cover.alignmentX = LEFT_ALIGNMENT
        cover.maximumSize = Dimension(380, 380)
        content.add(cover)
        content.add(Box.createVerticalStrut(22))

        title.foreground = Theme.text; title.font = Theme.font(26f, bold = true); title.alignmentX = LEFT_ALIGNMENT
        artist.foreground = Theme.textDim; artist.font = Theme.font(16f); artist.alignmentX = LEFT_ALIGNMENT
        status.foreground = Theme.accent; status.font = Theme.font(12f, bold = true); status.alignmentX = LEFT_ALIGNMENT
        title.maximumSize = Dimension(380, 34)
        content.add(title)
        content.add(Box.createVerticalStrut(6))
        content.add(artist)
        content.add(Box.createVerticalStrut(12))
        content.add(status)
        content.add(Box.createVerticalStrut(18))

        seek.alignmentX = LEFT_ALIGNMENT
        seek.maximumSize = Dimension(380, 22)
        content.add(seek)
        val times = JPanel(BorderLayout()).apply { isOpaque = false; alignmentX = LEFT_ALIGNMENT; maximumSize = Dimension(380, 18) }
        posLabel.foreground = Theme.textDim; posLabel.font = Theme.font(12f)
        durLabel.foreground = Theme.textDim; durLabel.font = Theme.font(12f)
        times.add(posLabel, BorderLayout.WEST); times.add(durLabel, BorderLayout.EAST)
        content.add(times)

        // Vertically center the column.
        return JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(400, 100)
            add(Box.createVerticalGlue())
            add(content)
            add(Box.createVerticalGlue())
        }
    }

    fun setPalette(colors: List<Color>) {
        val c1 = colors.firstOrNull() ?: Theme.surface
        val c2 = colors.getOrNull(1) ?: c1
        grad1 = Theme.mix(c1, Theme.bg, 0.34)
        grad2 = Theme.mix(c2, Theme.bg, 0.70)
        lyrics.setGradient(grad1, grad2)
        repaint()
    }

    fun setTrack(song: Song?) {
        cover.setSong(song)
        title.text = song?.title ?: "Nothing playing"
        artist.text = song?.artists ?: ""
        lyrics.setState(LyricsState.Unavailable)
        grad1 = Theme.surface; grad2 = Theme.bg
        lyrics.setGradient(grad1, grad2)
        repaint()
    }

    fun setLyrics(state: LyricsState) = lyrics.setState(state)

    fun setState(playing: Boolean, paused: Boolean) {
        lyrics.setPlaying(playing && !paused)
        status.text = when {
            playing && !paused -> "● Playing"
            paused -> "Paused"
            else -> "Stopped"
        }
        status.foreground = if (playing && !paused) Theme.accent else Theme.textDim
    }

    fun setProgress(pos: Double, dur: Double) {
        durationSec = dur
        seek.fraction = if (dur > 0) pos / dur else 0.0
        posLabel.text = mmss(pos)
        durLabel.text = mmss(dur)
        lyrics.setPositionMs((pos * 1000).toLong())
    }
}
