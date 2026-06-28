package com.metrolist.desktop.ui

import com.metrolist.desktop.model.Song
import com.metrolist.desktop.ui.components.CoverView
import com.metrolist.desktop.ui.components.SearchField
import com.metrolist.desktop.ui.theme.Theme
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class HomePanel(
    private val onSearch: (String) -> Unit,
    private val onPlaySong: (Song) -> Unit,
    private val onPlayDownloadedAt: (Int) -> Unit,
) : JPanel() {

    private val recentsRow = JPanel(FlowLayout(FlowLayout.LEFT, 16, 0)).apply { isOpaque = false }
    private val downloadsRow = JPanel(FlowLayout(FlowLayout.LEFT, 16, 0)).apply { isOpaque = false }
    private val downloadsLabel = JLabel("Downloaded songs")

    init {
        background = Theme.bg
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(40, 40, 24, 40)

        add(JLabel("Good vibes").apply { foreground = Theme.text; font = Theme.font(26f, bold = true); alignmentX = LEFT_ALIGNMENT })
        add(Box.createVerticalStrut(18))

        val search = SearchField("Search music").apply {
            maximumSize = Dimension(Int.MAX_VALUE, 50); alignmentX = LEFT_ALIGNMENT
            onSubmit = { q -> if (q.isNotBlank()) onSearch(q) }
        }
        add(search)
        add(Box.createVerticalStrut(30))

        add(heading("Recently played"))
        add(Box.createVerticalStrut(14))
        recentsRow.alignmentX = LEFT_ALIGNMENT
        add(recentsRow)
        add(Box.createVerticalStrut(28))

        downloadsLabel.apply { foreground = Theme.text; font = Theme.font(18f, bold = true); alignmentX = LEFT_ALIGNMENT }
        add(downloadsLabel)
        add(Box.createVerticalStrut(14))
        downloadsRow.alignmentX = LEFT_ALIGNMENT
        add(downloadsRow)
        add(Box.createVerticalGlue())

        setRecents(emptyList())
        setDownloads(emptyList())
    }

    private fun heading(text: String) = JLabel(text).apply {
        foreground = Theme.text; font = Theme.font(18f, bold = true); alignmentX = LEFT_ALIGNMENT
    }

    fun setRecents(songs: List<Song>) {
        recentsRow.removeAll()
        if (songs.isEmpty()) {
            recentsRow.add(hint("Nothing yet — search and play something."))
        } else {
            songs.take(6).forEach { s -> recentsRow.add(card(s) { onPlaySong(s) }) }
        }
        recentsRow.revalidate(); recentsRow.repaint()
    }

    fun setDownloads(songs: List<Song>) {
        downloadsRow.removeAll()
        if (songs.isEmpty()) {
            downloadsRow.add(hint("Download songs to play them offline."))
        } else {
            songs.take(6).forEachIndexed { i, s -> downloadsRow.add(card(s) { onPlayDownloadedAt(i) }) }
        }
        downloadsRow.revalidate(); downloadsRow.repaint()
    }

    private fun hint(text: String) = JLabel(text).apply { foreground = Theme.textDim; font = Theme.font(13f) }

    private fun card(song: Song, onClick: () -> Unit): JComponent {
        val card = JPanel().apply {
            isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(130, 180); cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        val cover = CoverView(130, 12).apply { setSong(song); alignmentX = LEFT_ALIGNMENT }
        card.add(cover); card.add(Box.createVerticalStrut(8))
        card.add(JLabel(ellipsize(song.title, 16)).apply { foreground = Theme.text; font = Theme.font(13f, bold = true); alignmentX = LEFT_ALIGNMENT })
        card.add(JLabel(ellipsize(song.artists, 18)).apply { foreground = Theme.textDim; font = Theme.font(12f); alignmentX = LEFT_ALIGNMENT })
        card.addMouseListener(object : MouseAdapter() { override fun mouseClicked(e: MouseEvent) { onClick() } })
        return card
    }

    private fun ellipsize(s: String, max: Int) = if (s.length <= max) s else s.take(max - 1) + "…"
}
