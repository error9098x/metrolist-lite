package com.metrolist.desktop.ui

import com.metrolist.desktop.lyrics.LyricLine
import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.lyrics.WordTiming
import com.metrolist.desktop.ui.theme.Theme
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.event.MouseWheelEvent
import java.awt.font.TextAttribute
import java.awt.geom.Point2D
import javax.swing.JComponent
import javax.swing.Timer

/**
 * Flowing, karaoke-style lyrics. The active line is emphasised and centered; neighbouring lines fade
 * out and dissolve into the background at the edges. Auto-scrolls to the playback position.
 *
 * [compact] = a small, transparent variant for embedding in the Now-Playing panel (a few bold
 * lines); the full variant paints its own album-art gradient + edge fade and is wheel-scrollable.
 */
class LyricsView(
    private val compact: Boolean = false,
    private val paintBackground: Boolean = !compact,
) : JComponent() {
    private var state: LyricsState = LyricsState.Unavailable
    private var currentIndex = 0
    private val lineHeight = if (compact) 38.0 else 56.0

    private var animScroll = 0.0
    private var targetScroll = 0.0
    private val animator = Timer(15) { stepAnim() }

    // Word-by-word highlighting state.
    private var positionMs = 0L
    private var posWall = System.currentTimeMillis()
    private var playing = false
    private val wordTimer = Timer(16) { if (hasActiveWords()) repaint() } // ~60fps

    private var grad1: Color = Theme.bg
    private var grad2: Color = Theme.bg

    init {
        isOpaque = false
        if (!compact) {
            addMouseWheelListener { e: MouseWheelEvent ->
                val l = (state as? LyricsState.Loaded) ?: return@addMouseWheelListener
                if (!l.synced) {
                    targetScroll = (targetScroll + e.preciseWheelRotation * 40).coerceIn(0.0, l.lines.size * lineHeight)
                    ensureAnimating()
                }
            }
        }
    }

    fun setState(state: LyricsState) {
        this.state = state
        currentIndex = 0
        animScroll = 0.0; targetScroll = 0.0
        wordTimer.stop()
        repaint()
    }

    fun setPlaying(value: Boolean) { playing = value }

    fun setPositionMs(ms: Long) {
        positionMs = ms
        posWall = System.currentTimeMillis()
        val l = (state as? LyricsState.Loaded) ?: return
        if (!l.synced) return
        var idx = l.lines.indexOfLast { it.timeMs in 0..ms }
        if (idx < 0) idx = 0
        if (idx != currentIndex) {
            currentIndex = idx
            targetScroll = currentIndex * lineHeight
            ensureAnimating()
        }
        if (hasActiveWords()) { if (!wordTimer.isRunning) wordTimer.start() } else wordTimer.stop()
    }

    private fun hasActiveWords(): Boolean =
        (state as? LyricsState.Loaded)?.lines?.getOrNull(currentIndex)?.words?.isNotEmpty() == true

    /** Smoothly extrapolate position between 500ms ticks so words light up continuously. */
    private fun estimatedMs(): Long =
        if (playing) positionMs + (System.currentTimeMillis() - posWall).coerceAtLeast(0) else positionMs

    fun setPalette(colors: List<Color>) {
        val c1 = colors.firstOrNull() ?: Theme.bg
        val c2 = colors.getOrNull(1) ?: c1
        grad1 = Theme.mix(c1, Theme.bg, 0.42)
        grad2 = Theme.mix(c2, Theme.bg, 0.80)
        repaint()
    }

    /** Use externally-computed gradient stops (so edge fades match a parent's background exactly). */
    fun setGradient(top: Color, bottom: Color) {
        grad1 = top; grad2 = bottom; repaint()
    }

    private fun ensureAnimating() { if (!animator.isRunning) animator.start() }

    private fun stepAnim() {
        animScroll += (targetScroll - animScroll) * 0.20
        if (kotlin.math.abs(targetScroll - animScroll) < 0.4) { animScroll = targetScroll; animator.stop() }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        if (paintBackground) {
            g2.paint = GradientPaint(0f, 0f, grad1, 0f, height.toFloat(), grad2)
            g2.fillRect(0, 0, width, height)
        }
        when (val s = state) {
            is LyricsState.Loading -> if (!compact) centered(g2, "Searching lyrics…", Theme.textDim, 16f)
            is LyricsState.Unavailable -> if (!compact) centered(g2, "No lyrics found for this track.", Theme.textDim, 16f)
            is LyricsState.Loaded -> if (s.synced) paintSynced(g2, s) else paintPlain(g2, s)
        }
        g2.dispose()
    }

    private val padX = if (compact) 2 else 40
    private val WEIGHT_REGULAR: Float = TextAttribute.WEIGHT_REGULAR
    private val WEIGHT_SEMI: Float = TextAttribute.WEIGHT_MEDIUM
    private val WEIGHT_BOLD: Float = TextAttribute.WEIGHT_BOLD

    private fun paintSynced(g2: Graphics2D, s: LyricsState.Loaded) {
        val centerY = if (compact) height * 0.30 else height * 0.46
        s.lines.forEachIndexed { i, line ->
            val active = i == currentIndex
            val dist = kotlin.math.abs(i - currentIndex)
            var y = centerY + (i * lineHeight - animScroll)
            if (y < -lineHeight * 2 || y > height + lineHeight * 2) return@forEachIndexed
            val size = if (compact) (if (active) 21f else 17f) else (if (active) 31f else 25f)

            if (active && !compact) {
                if (line.words.isNotEmpty()) { drawKaraokeLine(g2, line, y, size); return@forEachIndexed }
                drawLine(g2, line.text, y, Theme.lyricSung, size, WEIGHT_BOLD)
                return@forEachIndexed
            }

            val weight = if (compact && active) WEIGHT_BOLD else WEIGHT_SEMI
            val alpha = if (active) 0.9 else (0.34 - dist * 0.05).coerceIn(0.18, 0.34)
            drawLine(g2, line.text, y, fade(Theme.lyricSung, alpha), size, weight)
        }
    }

    private fun paintPlain(g2: Graphics2D, s: LyricsState.Loaded) {
        val top = if (compact) 6.0 else 48.0
        s.lines.forEachIndexed { i, line ->
            val y = top + i * (lineHeight * 0.62) - animScroll
            if (y < -lineHeight || y > height + lineHeight) return@forEachIndexed
            drawLine(g2, line.text, y, Theme.text, if (compact) 16f else 22f, WEIGHT_REGULAR)
        }
    }

    /** Fade lines toward the top/bottom edges via alpha so they dissolve into whatever is behind. */
    private fun edgeFactor(y: Double): Double {
        val margin = height * 0.18
        if (margin <= 0) return 1.0
        return (minOf(y, height - y) / margin).coerceIn(0.0, 1.0)
    }

    /**
     * Active word-synced line drawn word-by-word: already-sung words are light pastel, upcoming are
     * dim. The word being sung *right now* fills sub-word (left→right) with a bright pastel and a
     * brighter "pointer" at the fill front, and gently bobs up (smooth, ~60fps).
     */
    private fun drawKaraokeLine(g2: Graphics2D, line: LyricLine, y: Double, size: Float) {
        g2.font = Theme.font(size, bold = false).deriveFont(mapOf(TextAttribute.WEIGHT to WEIGHT_BOLD))
        val fm = g2.fontMetrics
        val spaceW = fm.stringWidth(" ").toDouble()
        val baseY = y + fm.ascent / 2
        val ms = estimatedMs()
        val ef0 = edgeFactor(baseY)

        var x = padX.toDouble()
        for (w in line.words) {
            val ww = fm.stringWidth(w.text).toDouble()
            val x0 = x; val x1 = x + ww
            when {
                ms >= w.endMs -> { g2.color = withAlpha(Theme.lyricSung, ef0); g2.drawString(w.text, x0.toInt(), baseY.toInt()) }
                ms < w.startMs -> { g2.color = withAlpha(Theme.lyricSung, 0.30 * ef0); g2.drawString(w.text, x0.toInt(), baseY.toInt()) }
                else -> {
                    val p = if (w.endMs > w.startMs) (ms - w.startMs).toDouble() / (w.endMs - w.startMs) else 1.0
                    // Smooth, very subtle bob of the word being sung (rises in, settles out).
                    val lift = kotlin.math.sin(p.coerceIn(0.0, 1.0) * Math.PI) * 2.2
                    val by = baseY - lift
                    val ef = edgeFactor(by)
                    val paint = wordFill(x0, x1, by, p, ef)
                    if (paint != null) g2.paint = paint else g2.color = withAlpha(Theme.lyricFront, ef)
                    g2.drawString(w.text, x0.toInt(), by.toInt())
                }
            }
            x = x1 + spaceW
        }
    }

    /** Horizontal pastel gradient filling a single word up to fraction [frac]. */
    private fun wordFill(x0: Double, x1: Double, baseline: Double, frac: Double, ef: Double): LinearGradientPaint? {
        val w = x1 - x0
        if (w < 1.0) return null
        val sung = withAlpha(Theme.lyricSung, ef)
        val front = withAlpha(Theme.lyricFront, ef)
        val idle = withAlpha(Theme.lyricSung, 0.32 * ef)
        val f = frac.coerceIn(0.0, 1.0)
        val band = (10.0 / w).coerceIn(0.04, 0.45)
        val raw = listOf(0.0 to sung, (f - band) to sung, f to front, (f + band) to idle, 1.0 to idle)
        val fracs = ArrayList<Float>(); val cols = ArrayList<Color>()
        var last = -1.0
        for ((f0, c) in raw) {
            var ff = f0.coerceIn(0.0, 1.0)
            if (ff <= last) ff = last + 0.0005
            if (ff > 1.0) ff = 1.0
            if (ff <= last) continue
            fracs.add(ff.toFloat()); cols.add(c); last = ff
        }
        if (fracs.size < 2) return null
        return LinearGradientPaint(
            Point2D.Float(x0.toFloat(), baseline.toFloat()),
            Point2D.Float(x1.toFloat(), baseline.toFloat()),
            fracs.toFloatArray(), cols.toTypedArray(),
        )
    }

    private fun withAlpha(c: Color, f: Double) = Color(c.red, c.green, c.blue, (c.alpha * f).toInt().coerceIn(0, 255))

    private fun drawLine(g2: Graphics2D, text: String, y: Double, color: Color, size: Float, weight: Float) {
        g2.font = Theme.font(size, bold = false).deriveFont(mapOf(TextAttribute.WEIGHT to weight))
        val a = (color.alpha * edgeFactor(y)).toInt().coerceIn(0, 255)
        g2.color = Color(color.red, color.green, color.blue, a)
        val fm = g2.fontMetrics
        val shown = ellipsize(text, fm, width - padX - 24)
        g2.drawString(shown, padX, (y + fm.ascent / 2).toInt())
    }

    private fun ellipsize(text: String, fm: java.awt.FontMetrics, maxW: Int): String {
        if (fm.stringWidth(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && fm.stringWidth("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun centered(g2: Graphics2D, text: String, color: Color, size: Float) {
        g2.font = Theme.font(size); g2.color = color
        val fm = g2.fontMetrics
        g2.drawString(text, (width - fm.stringWidth(text)) / 2, height / 2)
    }

    private fun fade(c: Color, a: Double) = Color(c.red, c.green, c.blue, (a * 255).toInt().coerceIn(0, 255))
}
