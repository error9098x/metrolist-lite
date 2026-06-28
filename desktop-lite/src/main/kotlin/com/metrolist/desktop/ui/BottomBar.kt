package com.metrolist.desktop.ui

import com.metrolist.desktop.model.Song
import com.metrolist.desktop.playback.RepeatMode
import com.metrolist.desktop.ui.components.CoverView
import com.metrolist.desktop.ui.components.IconButton
import com.metrolist.desktop.ui.components.SeekBar
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class BottomBar(
    private val controls: Controls,
    private val onOpenNowPlaying: () -> Unit = {},
) : JPanel() {

    private val cover = CoverView(48, 8)
    private val title = JLabel("—")
    private val artist = JLabel("")
    private val playPause = IconButton("play", box = 16, pad = 10).apply { filled = true }
    private val shuffle = IconButton("shuffle", box = 16, pad = 9)
    private val repeat = IconButton("repeat", box = 16, pad = 9)
    private val seek = SeekBar(knobRadius = 6)
    private val posLabel = JLabel("0:00")
    private val durLabel = JLabel("0:00")
    private val volume = SeekBar(knobRadius = 5).apply { fraction = 1.0 }

    private var durationSec = 0.0

    init {
        background = Theme.sidebar
        layout = BorderLayout(16, 0)
        border = BorderFactory.createEmptyBorder(12, 20, 12, 20)
        preferredSize = Dimension(100, 92)

        add(left(), BorderLayout.WEST)
        add(center(), BorderLayout.CENTER)
        add(right(), BorderLayout.EAST)

        playPause.onClick = { controls.togglePlay() }
        shuffle.onClick = { controls.toggleShuffle() }
        repeat.onClick = { controls.cycleRepeat() }
        seek.onSeek = { frac -> if (durationSec > 0) controls.seek(frac * durationSec) }
        volume.onSeek = { frac -> controls.setVolume((frac * 100).toInt()) }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.color = Theme.divider
        g2.fillRect(0, 0, width, 1)
    }

    private fun left(): JPanel {
        val p = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.X_AXIS) }
        p.preferredSize = Dimension(260, 68)
        cover.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        cover.toolTipText = "Open Now Playing"
        cover.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { onOpenNowPlaying() }
        })
        p.add(cover)
        p.add(Box.createHorizontalStrut(12))
        val meta = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        title.foreground = Theme.text; title.font = Theme.font(13f, bold = true); title.alignmentX = LEFT_ALIGNMENT
        artist.foreground = Theme.textDim; artist.font = Theme.font(12f); artist.alignmentX = LEFT_ALIGNMENT
        meta.add(Box.createVerticalGlue()); meta.add(title); meta.add(Box.createVerticalStrut(3)); meta.add(artist); meta.add(Box.createVerticalGlue())
        p.add(meta)
        return p
    }

    private fun center(): JPanel {
        val col = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS) }

        val transport = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.X_AXIS) }
        transport.add(Box.createHorizontalGlue())
        transport.add(shuffle)
        transport.add(Box.createHorizontalStrut(6))
        transport.add(IconButton("prev", box = 18, pad = 9).also { it.onClick = { controls.prev() } })
        transport.add(Box.createHorizontalStrut(6))
        transport.add(playPause)
        transport.add(Box.createHorizontalStrut(6))
        transport.add(IconButton("next", box = 18, pad = 9).also { it.onClick = { controls.next() } })
        transport.add(Box.createHorizontalStrut(6))
        transport.add(repeat)
        transport.add(Box.createHorizontalGlue())

        val progress = JPanel(BorderLayout(8, 0)).apply { isOpaque = false; maximumSize = Dimension(Int.MAX_VALUE, 20) }
        posLabel.foreground = Theme.textDim; posLabel.font = Theme.font(11f)
        durLabel.foreground = Theme.textDim; durLabel.font = Theme.font(11f)
        progress.add(posLabel, BorderLayout.WEST)
        progress.add(seek, BorderLayout.CENTER)
        progress.add(durLabel, BorderLayout.EAST)

        col.add(transport)
        col.add(Box.createVerticalStrut(2))
        col.add(progress)
        return col
    }

    private fun right(): JPanel {
        val p = JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.X_AXIS) }
        p.preferredSize = Dimension(180, 68)
        p.add(Box.createHorizontalGlue())
        val vol = IconButton("volume", box = 18, pad = 8); vol.color = Theme.textDim
        p.add(vol)
        p.add(Box.createHorizontalStrut(6))
        volume.preferredSize = Dimension(100, 22); volume.maximumSize = Dimension(100, 22)
        p.add(volume)
        return p
    }

    fun setTrack(song: Song?) {
        cover.setSong(song)
        title.text = song?.title ?: "—"
        artist.text = song?.artists ?: ""
    }

    fun setState(playing: Boolean, paused: Boolean) {
        playPause.iconName = if (playing && !paused) "pause" else "play"
        playPause.repaint()
    }

    fun setProgress(pos: Double, dur: Double) {
        durationSec = dur
        seek.fraction = if (dur > 0) pos / dur else 0.0
        posLabel.text = mmss(pos)
        durLabel.text = mmss(dur)
    }

    fun setModes(shuffleOn: Boolean, repeatMode: RepeatMode) {
        shuffle.active = shuffleOn; shuffle.repaint()
        repeat.active = repeatMode != RepeatMode.NONE
        repeat.color = if (repeatMode == RepeatMode.ONE) Theme.coral else Theme.textDim
        repeat.repaint()
    }
}
