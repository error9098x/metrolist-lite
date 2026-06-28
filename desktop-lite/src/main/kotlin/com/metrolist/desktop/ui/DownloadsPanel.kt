package com.metrolist.desktop.ui

import com.metrolist.desktop.download.DownloadStatus
import com.metrolist.desktop.download.DownloadedSong
import com.metrolist.desktop.ui.components.CoverView
import com.metrolist.desktop.ui.components.IconButton
import com.metrolist.desktop.ui.components.RoundedPanel
import com.metrolist.desktop.ui.theme.Theme
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

/** Downloads tab: in-progress items on top, then the offline library (click to play). */
class DownloadsPanel(
    private val onPlayAt: (Int) -> Unit,
    private val onRemove: (String) -> Unit,
) : JPanel() {

    private val list = JPanel()

    init {
        background = Theme.bg
        layout = java.awt.BorderLayout(0, 14)
        border = BorderFactory.createEmptyBorder(34, 28, 16, 28)

        add(JLabel("Downloads").apply { foreground = Theme.text; font = Theme.font(22f, bold = true) }, java.awt.BorderLayout.NORTH)

        list.background = Theme.bg
        list.layout = BoxLayout(list, BoxLayout.Y_AXIS)
        add(JScrollPane(list).apply {
            border = null; viewport.background = Theme.bg
            verticalScrollBar.unitIncrement = 18
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }, java.awt.BorderLayout.CENTER)
    }

    fun refresh(downloaded: List<DownloadedSong>, inProgress: Map<String, DownloadStatus>) {
        list.removeAll()
        val active = inProgress.filterValues { it is DownloadStatus.Downloading || it is DownloadStatus.Queued || it is DownloadStatus.Failed }
        if (active.isNotEmpty()) {
            list.add(section("In progress"))
            active.forEach { (_, st) -> list.add(progressRow(st)) }
            list.add(Box.createVerticalStrut(10))
        }
        list.add(section("Downloaded songs · ${downloaded.size}"))
        if (downloaded.isEmpty()) {
            list.add(JLabel("Nothing downloaded yet. Tap the download icon on a search result.").apply {
                foreground = Theme.textDim; font = Theme.font(13f); border = BorderFactory.createEmptyBorder(10, 8, 0, 0)
            })
        } else {
            downloaded.forEachIndexed { i, d ->
                list.add(downloadedRow(d, i)); list.add(Box.createVerticalStrut(2))
            }
        }
        list.revalidate(); list.repaint()
    }

    private fun section(text: String): JComponent = JLabel(text).apply {
        foreground = Theme.textFaint; font = Theme.font(12f, bold = true)
        alignmentX = LEFT_ALIGNMENT; border = BorderFactory.createEmptyBorder(4, 6, 6, 0)
        maximumSize = Dimension(Int.MAX_VALUE, 24)
    }

    private fun progressRow(status: DownloadStatus): JComponent {
        val label = when (status) {
            is DownloadStatus.Downloading -> "Downloading… ${status.percent}%"
            is DownloadStatus.Queued -> "Queued…"
            is DownloadStatus.Failed -> "Failed: ${status.message}"
            else -> ""
        }
        return JPanel().apply {
            isOpaque = false; layout = BoxLayout(this, BoxLayout.X_AXIS)
            maximumSize = Dimension(Int.MAX_VALUE, 32); alignmentX = LEFT_ALIGNMENT
            add(JLabel(label).apply {
                foreground = if (status is DownloadStatus.Failed) Theme.coral else Theme.accent; font = Theme.font(13f)
            })
        }
    }

    private fun downloadedRow(d: DownloadedSong, index: Int): JComponent {
        val row = RoundedPanel(Theme.ROW_RADIUS, null)
        row.layout = null
        row.maximumSize = Dimension(Int.MAX_VALUE, 58)
        row.preferredSize = Dimension(600, 58)
        row.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        val cover = CoverView(42, 8); cover.setSong(d.toSong())
        val title = JLabel(d.title).apply { foreground = Theme.text; font = Theme.font(14f, bold = true) }
        val artist = JLabel(d.artists).apply { foreground = Theme.textDim; font = Theme.font(12f) }
        val check = IconButton("check", box = 15, pad = 9, color = Theme.accent).apply { active = true; toolTipText = "Available offline" }
        val trash = IconButton("trash", box = 16, pad = 9, color = Theme.textDim).apply { onClick = { onRemove(d.videoId) }; toolTipText = "Remove download" }
        val play = IconButton("play", box = 16, pad = 9, color = Theme.textDim).apply { onClick = { onPlayAt(index) } }
        listOf<JComponent>(cover, title, artist, check, trash, play).forEach { row.add(it) }

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.bg = Theme.surfaceHover; row.repaint() }
            override fun mouseExited(e: MouseEvent) { if (!row.contains(e.point)) { row.bg = null; row.repaint() } }
            override fun mouseClicked(e: MouseEvent) { onPlayAt(index) }
        })
        row.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                val h = row.height
                cover.setBounds(8, (h - 42) / 2, 42, 42)
                title.setBounds(62, h / 2 - 18, row.width - 180, 20)
                artist.setBounds(62, h / 2 + 2, row.width - 180, 18)
                check.setBounds(row.width - 128, (h - 33) / 2, 33, 33)
                trash.setBounds(row.width - 88, (h - 34) / 2, 34, 34)
                play.setBounds(row.width - 46, (h - 34) / 2, 34, 34)
            }
        })
        return row
    }
}
