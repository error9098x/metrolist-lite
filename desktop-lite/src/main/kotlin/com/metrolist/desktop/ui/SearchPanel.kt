package com.metrolist.desktop.ui

import com.metrolist.desktop.download.DownloadStatus
import com.metrolist.desktop.model.Song
import com.metrolist.desktop.ui.components.CoverView
import com.metrolist.desktop.ui.components.IconButton
import com.metrolist.desktop.ui.components.RoundedPanel
import com.metrolist.desktop.ui.components.SearchField
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
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

class SearchPanel(
    private val onSearch: (String) -> Unit,
    private val onPlayAt: (Int) -> Unit,
    private val onDownload: (Song) -> Unit,
    private val statusOf: (String) -> DownloadStatus,
) : JPanel() {

    private val results = mutableListOf<Song>()
    private val rowsById = HashMap<String, SongRow>()
    private val list = JPanel()
    private val statusLabel = JLabel("Search for a song to begin.")
    private val searchField = SearchField("Search music")

    init {
        background = Theme.bg
        layout = BorderLayout(0, 16)
        border = BorderFactory.createEmptyBorder(34, 28, 16, 28)

        searchField.onSubmit = { q -> if (q.isNotBlank()) onSearch(q) }
        add(searchField, BorderLayout.NORTH)

        list.background = Theme.bg
        list.layout = BoxLayout(list, BoxLayout.Y_AXIS)

        val scroll = JScrollPane(list).apply {
            border = null
            viewport.background = Theme.bg
            verticalScrollBar.unitIncrement = 18
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        add(scroll, BorderLayout.CENTER)

        statusLabel.foreground = Theme.textDim
        statusLabel.font = Theme.font(13f)
        add(statusLabel, BorderLayout.SOUTH)
    }

    fun focusSearch() = searchField.field.requestFocusInWindow()
    fun setQueryText(q: String) { searchField.field.text = q }
    fun setStatus(message: String) { statusLabel.text = message }

    fun updateDownload(videoId: String, status: DownloadStatus) {
        rowsById[videoId]?.setDownloadState(status)
    }

    fun showResults(query: String, songs: List<Song>) {
        results.clear(); results.addAll(songs)
        rowsById.clear()
        list.removeAll()
        list.add(header())
        songs.forEachIndexed { i, song ->
            val row = SongRow(song, i, onPlayAt, onDownload, statusOf(song.videoId))
            rowsById[song.videoId] = row
            list.add(row)
            list.add(Box.createVerticalStrut(2))
        }
        if (songs.isEmpty()) {
            list.add(JLabel("No results for \"$query\".").apply {
                foreground = Theme.textDim; font = Theme.font(14f); border = BorderFactory.createEmptyBorder(20, 8, 0, 0)
            })
        }
        list.revalidate(); list.repaint()
    }

    private fun header(): JComponent {
        val h = JPanel(BorderLayout())
        h.isOpaque = false
        h.maximumSize = Dimension(Int.MAX_VALUE, 30)
        h.border = BorderFactory.createEmptyBorder(0, 56, 8, 110)
        h.add(JLabel("Title").apply { foreground = Theme.textFaint; font = Theme.font(12f) }, BorderLayout.WEST)
        h.add(JLabel("Duration").apply { foreground = Theme.textFaint; font = Theme.font(12f) }, BorderLayout.EAST)
        return h
    }
}

/** Result row: cover + title/artist + duration + download + play, with hover + dbl-click. */
class SongRow(
    private val song: Song,
    private val index: Int,
    private val onPlayAt: (Int) -> Unit,
    private val onDownload: (Song) -> Unit,
    initialStatus: DownloadStatus,
) : RoundedPanel(Theme.ROW_RADIUS, null) {

    private val cover = CoverView(44, 8)
    private val title = JLabel(song.title)
    private val artist = JLabel(song.artists)
    private val duration = JLabel(song.durationText)
    private val download = IconButton("download", box = 17, pad = 9, color = Theme.textDim)
    private val play = IconButton("play", box = 16, pad = 9, color = Theme.textDim)

    init {
        layout = null
        maximumSize = Dimension(Int.MAX_VALUE, 60)
        preferredSize = Dimension(600, 60)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        cover.setSong(song)
        cover.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        cover.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { onPlayAt(index) }
        })
        title.foreground = Theme.text; title.font = Theme.font(14f, bold = true)
        artist.foreground = Theme.textDim; artist.font = Theme.font(12f)
        duration.foreground = Theme.textDim; duration.font = Theme.font(13f)
        play.onClick = { onPlayAt(index) }
        download.onClick = { onDownload(song) }
        listOf(cover, title, artist, duration, download, play).forEach { add(it) }
        setDownloadState(initialStatus)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { bg = Theme.surfaceHover; repaint() }
            override fun mouseExited(e: MouseEvent) { if (!contains(e.point)) { bg = null; repaint() } }
            override fun mouseClicked(e: MouseEvent) { if (e.clickCount == 2) onPlayAt(index) }
        })
    }

    fun setDownloadState(status: DownloadStatus) {
        when (status) {
            is DownloadStatus.Completed -> { download.iconName = "check"; download.color = Theme.accent; download.active = true; download.toolTipText = "Downloaded" }
            is DownloadStatus.Downloading -> { download.iconName = "download"; download.color = Theme.accent; download.active = false; download.toolTipText = "Downloading ${status.percent}%" }
            is DownloadStatus.Queued -> { download.iconName = "download"; download.color = Theme.accent; download.toolTipText = "Queued…" }
            is DownloadStatus.Failed -> { download.iconName = "download"; download.color = Theme.coral; download.toolTipText = status.message }
            DownloadStatus.None -> { download.iconName = "download"; download.color = Theme.textDim; download.active = false; download.toolTipText = "Download" }
        }
        download.repaint()
    }

    override fun doLayout() {
        val h = height
        cover.setBounds(8, (h - 44) / 2, 44, 44)
        val textX = 64
        title.setBounds(textX, h / 2 - 18, width - textX - 200, 20)
        artist.setBounds(textX, h / 2 + 2, width - textX - 200, 18)
        duration.setBounds(width - 168, h / 2 - 9, 60, 18)
        download.setBounds(width - 88, (h - 34) / 2, 34, 34)
        play.setBounds(width - 46, (h - 34) / 2, 34, 34)
    }
}
