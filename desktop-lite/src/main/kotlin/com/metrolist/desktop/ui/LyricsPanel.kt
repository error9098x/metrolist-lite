package com.metrolist.desktop.ui

import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel

/** Full-screen Lyrics tab: flowing, art-gradient lyrics. */
class LyricsPanel : JPanel() {
    private val view = LyricsView(compact = false)

    init {
        background = Theme.bg
        layout = BorderLayout()
        add(view, BorderLayout.CENTER)
    }

    fun setState(state: LyricsState) = view.setState(state)
    fun setPositionMs(ms: Long) = view.setPositionMs(ms)
    fun setPalette(colors: List<Color>) = view.setPalette(colors)
}
