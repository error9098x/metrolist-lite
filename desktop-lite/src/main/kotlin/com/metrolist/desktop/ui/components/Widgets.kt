package com.metrolist.desktop.ui.components

import com.metrolist.desktop.model.Song
import com.metrolist.desktop.ui.ImageLoader
import com.metrolist.desktop.ui.theme.Icons
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

private fun Graphics2D.aa() {
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
}

/** Panel with a solid rounded background. */
open class RoundedPanel(private val radius: Int, var bg: Color?) : JPanel() {
    init { isOpaque = false }
    override fun paintComponent(g: Graphics) {
        bg?.let {
            val g2 = g.create() as Graphics2D
            g2.aa(); g2.color = it
            g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat()))
            g2.dispose()
        }
        super.paintComponent(g)
    }
}

/** Vector icon button with hover state, optional accent fill, and active (teal) toggle. */
class IconButton(
    var iconName: String,
    private val box: Int = 18,
    private val pad: Int = 9,
    var color: Color = Theme.textDim,
) : JComponent() {
    var onClick: () -> Unit = {}
    var active = false
    var filled = false
    var fillColor: Color = Theme.accent
    private var hover = false

    init {
        val s = box + pad * 2
        preferredSize = Dimension(s, s)
        // Never let a layout stretch the button: keeps the circle a true circle.
        minimumSize = preferredSize
        maximumSize = preferredSize
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { hover = true; repaint() }
            override fun mouseExited(e: MouseEvent) { hover = false; repaint() }
            override fun mouseClicked(e: MouseEvent) { onClick() }
        })
    }

    override fun getMinimumSize(): Dimension = preferredSize
    override fun getMaximumSize(): Dimension = preferredSize

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.aa()
        // Circle centered within the component, sized to the smaller side (robust to any bounds).
        val d = minOf(width, height).toDouble()
        val cx = (width - d) / 2
        val cy = (height - d) / 2
        val iconBox = box.toDouble()
        val ix = (width - iconBox) / 2
        val iy = (height - iconBox) / 2
        if (filled) {
            g2.color = if (hover) fillColor.brighter() else fillColor
            g2.fill(Ellipse2D.Double(cx, cy, d, d))
            Icons.paint(g2, iconName, ix, iy, iconBox, Theme.onAccent)
        } else {
            if (hover) {
                g2.color = Theme.surfaceHover
                g2.fill(Ellipse2D.Double(cx, cy, d, d))
            }
            val c = when {
                active -> Theme.accent
                hover -> Theme.text
                else -> color
            }
            Icons.paint(g2, iconName, ix, iy, iconBox, c)
        }
        g2.dispose()
    }
}

/** Rounded album-art view with a placeholder, async-loaded from the song's thumbnail. */
class CoverView(private val px: Int, private val radius: Int) : JComponent() {
    private var img: BufferedImage? = null
    private var url: String? = null

    init { preferredSize = Dimension(px, px) }

    // Keep it a hard square in every layout (Box/GridBag won't stretch it).
    override fun getMinimumSize(): Dimension = preferredSize
    override fun getMaximumSize(): Dimension = preferredSize

    fun setSong(song: Song?) {
        url = song?.thumbnailUrl
        img = ImageLoader.cached(url, px)
        repaint()
        val want = url
        ImageLoader.load(want, px, radius) { loaded ->
            if (url == want) { img = loaded; repaint() }
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.aa()
        val w = width; val h = height
        val image = img
        if (image != null) {
            g2.drawImage(image, 0, 0, w, h, null)
        } else {
            g2.color = Theme.surfaceHover
            g2.fill(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), radius.toFloat(), radius.toFloat()))
            Icons.paint(g2, "note", w * 0.32, h * 0.32, w * 0.36, Theme.textFaint)
        }
        g2.dispose()
    }
}

/** Horizontal progress/seek bar. Reports a 0..1 fraction on click/drag. */
class SeekBar(private val knobRadius: Int = 6, var accent: Color = Theme.accent) : JComponent() {
    var fraction: Double = 0.0
        set(value) { field = value.coerceIn(0.0, 1.0); if (!dragging) repaint() }
    var onSeek: (Double) -> Unit = {}
    private var dragging = false
    private var hover = false

    init {
        preferredSize = Dimension(120, 22)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        val handler = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { dragging = true; update(e.x) }
            override fun mouseDragged(e: MouseEvent) { update(e.x) }
            override fun mouseReleased(e: MouseEvent) { dragging = false; onSeek(fraction) }
            override fun mouseEntered(e: MouseEvent) { hover = true; repaint() }
            override fun mouseExited(e: MouseEvent) { hover = false; repaint() }
        }
        addMouseListener(handler); addMouseMotionListener(handler)
    }

    private fun update(x: Int) {
        fraction = (x.toDouble() / width.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.aa()
        val cy = height / 2.0
        val th = 4.0
        val pad = knobRadius.toDouble()
        val trackW = width - pad * 2
        g2.color = Theme.divider
        g2.fill(RoundRectangle2D.Double(pad, cy - th / 2, trackW, th, th, th))
        val fillW = trackW * fraction
        g2.color = accent
        g2.fill(RoundRectangle2D.Double(pad, cy - th / 2, fillW, th, th, th))
        if (hover || dragging) {
            g2.color = Color.WHITE
            val kx = pad + fillW
            g2.fill(Ellipse2D.Double(kx - knobRadius, cy - knobRadius, knobRadius * 2.0, knobRadius * 2.0))
        }
        g2.dispose()
    }
}

/** Rounded search input with a magnifier glyph and Enter-to-submit. */
class SearchField(placeholder: String) : RoundedPanel(12, Theme.surface) {
    val field = object : JTextField() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (text.isEmpty() && !hasFocus()) {
                val g2 = g.create() as Graphics2D; g2.aa()
                g2.color = Theme.textFaint
                g2.font = Theme.font(15f)
                val fm = g2.fontMetrics
                g2.drawString(placeholder, 2, (height + fm.ascent - fm.descent) / 2)
                g2.dispose()
            }
        }
    }
    var onSubmit: (String) -> Unit = {}

    init {
        layout = BorderLayout(8, 0)
        border = BorderFactory.createEmptyBorder(10, 14, 10, 14)
        preferredSize = Dimension(420, 44)

        val icon = object : JComponent() {
            init { preferredSize = Dimension(20, 20) }
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D; g2.aa()
                Icons.paint(g2, "search", 0.0, 0.0, 20.0, Theme.textDim); g2.dispose()
            }
        }

        field.isOpaque = false
        field.border = null
        field.foreground = Theme.text
        field.caretColor = Theme.accent
        field.font = Theme.font(15f)
        field.putClientProperty("JTextField.placeholderText", placeholder)
        field.addActionListener { onSubmit(field.text) }
        field.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) = field.repaint()
            override fun focusLost(e: java.awt.event.FocusEvent) = field.repaint()
        })

        add(icon, BorderLayout.WEST)
        add(field, BorderLayout.CENTER)
    }
}
