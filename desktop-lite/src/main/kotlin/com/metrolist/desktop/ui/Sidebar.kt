package com.metrolist.desktop.ui

import com.metrolist.desktop.ui.theme.Icons
import com.metrolist.desktop.ui.theme.Theme
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

enum class Screen { HOME, SEARCH, DOWNLOADS, NOW_PLAYING }

class Sidebar(private val onNavigate: (Screen) -> Unit) : JPanel() {

    private val items = mutableListOf<NavItem>()

    init {
        background = Theme.sidebar
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        // Extra top inset clears the macOS traffic-light buttons (full-window-content mode).
        border = BorderFactory.createEmptyBorder(52, 16, 16, 16)
        preferredSize = Dimension(232, 100)

        add(logo())
        add(Box.createVerticalStrut(26))

        addNav(Screen.HOME, "home", "Home")
        addNav(Screen.SEARCH, "search", "Search")
        addNav(Screen.DOWNLOADS, "download", "Downloads")
        addNav(Screen.NOW_PLAYING, "wave", "Now Playing")

        add(Box.createVerticalGlue())
        select(Screen.HOME)
    }

    private fun logo(): JComponent {
        val row = JPanel(null)
        row.isOpaque = false
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.alignmentX = LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, 36)
        val mark = object : JComponent() {
            init { preferredSize = Dimension(26, 26); maximumSize = preferredSize }
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                Icons.paint(g2, "wave", 0.0, 0.0, 26.0, Theme.accent); g2.dispose()
            }
        }
        val title = JLabel("  Metrolist Lite")
        title.foreground = Theme.text
        title.font = Theme.font(17f, bold = true)
        row.add(mark); row.add(title)
        return row
    }

    private fun addNav(screen: Screen, icon: String, label: String) {
        val item = NavItem(icon, label) { select(screen); onNavigate(screen) }
        item.alignmentX = LEFT_ALIGNMENT
        items += item
        add(item)
        add(Box.createVerticalStrut(6))
    }

    fun select(screen: Screen) {
        items.forEachIndexed { i, it -> it.selected = (Screen.entries[i] == screen) }
    }
}

/** A single sidebar nav row: icon + label, with hover and selected (teal) states. */
class NavItem(
    private val icon: String,
    label: String,
    private val onClick: () -> Unit,
) : JComponent() {
    var selected = false
        set(value) { field = value; repaint() }
    private var hover = false

    init {
        layout = null
        maximumSize = Dimension(Int.MAX_VALUE, 44)
        preferredSize = Dimension(200, 44)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { hover = true; repaint() }
            override fun mouseExited(e: MouseEvent) { hover = false; repaint() }
            override fun mouseClicked(e: MouseEvent) { onClick() }
        })
    }

    private val text = label

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (selected || hover) {
            g2.color = if (selected) Theme.surfaceActive else Theme.surfaceHover
            g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 10f, 10f))
        }
        val c: Color = if (selected) Theme.accent else if (hover) Theme.text else Theme.textDim
        Icons.paint(g2, icon, 14.0, (height - 20) / 2.0, 20.0, c)
        g2.color = c
        g2.font = Theme.font(15f, bold = selected)
        val fm = g2.fontMetrics
        g2.drawString(text, 48, (height + fm.ascent - fm.descent) / 2)
        g2.dispose()
    }
}
