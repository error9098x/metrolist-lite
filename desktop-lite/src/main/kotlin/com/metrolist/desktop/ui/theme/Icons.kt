package com.metrolist.desktop.ui.theme

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D

/**
 * Crisp vector icons painted with Java2D (no icon-font/image assets). Each icon is drawn inside a
 * size×size box whose top-left is (x, y).
 */
object Icons {

    fun paint(g0: Graphics2D, name: String, x: Double, y: Double, size: Double, color: Color) {
        val g = g0.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.translate(x, y)
            g.color = color
            val s = size
            val stroke = BasicStroke((s * 0.085).toFloat().coerceAtLeast(1.4f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.stroke = stroke
            when (name) {
                "play" -> {
                    val p = GeneralPath()
                    p.moveTo(s * 0.28, s * 0.20); p.lineTo(s * 0.80, s * 0.50); p.lineTo(s * 0.28, s * 0.80); p.closePath()
                    g.fill(p)
                }
                "pause" -> {
                    val w = s * 0.16; val h = s * 0.62; val top = s * 0.19
                    g.fill(RoundRectangle2D.Double(s * 0.28, top, w, h, w * 0.5, w * 0.5))
                    g.fill(RoundRectangle2D.Double(s * 0.56, top, w, h, w * 0.5, w * 0.5))
                }
                "stop" -> g.fill(RoundRectangle2D.Double(s * 0.26, s * 0.26, s * 0.48, s * 0.48, s * 0.10, s * 0.10))
                "next", "prev" -> {
                    if (name == "prev") { g.translate(s, 0.0); g.scale(-1.0, 1.0) }
                    val p = GeneralPath()
                    p.moveTo(s * 0.24, s * 0.24); p.lineTo(s * 0.64, s * 0.50); p.lineTo(s * 0.24, s * 0.76); p.closePath()
                    g.fill(p)
                    g.fill(RoundRectangle2D.Double(s * 0.66, s * 0.24, s * 0.10, s * 0.52, s * 0.05, s * 0.05))
                }
                "shuffle" -> {
                    g.fill(arrowedLine(s * 0.18, s * 0.74, s * 0.82, s * 0.26, s))
                    g.fill(arrowedLine(s * 0.18, s * 0.26, s * 0.82, s * 0.74, s))
                }
                "repeat" -> {
                    val arc = Path2D.Double()
                    arc.append(java.awt.geom.Arc2D.Double(s * 0.20, s * 0.22, s * 0.60, s * 0.56, 60.0, 250.0, java.awt.geom.Arc2D.OPEN), false)
                    g.draw(arc)
                    val head = GeneralPath()
                    head.moveTo(s * 0.66, s * 0.14); head.lineTo(s * 0.82, s * 0.26); head.lineTo(s * 0.60, s * 0.34)
                    g.fill(head)
                }
                "heart" -> {
                    val p = GeneralPath()
                    p.moveTo(s * 0.50, s * 0.78)
                    p.curveTo(s * 0.08, s * 0.50, s * 0.18, s * 0.16, s * 0.50, s * 0.34)
                    p.curveTo(s * 0.82, s * 0.16, s * 0.92, s * 0.50, s * 0.50, s * 0.78)
                    p.closePath()
                    g.fill(p)
                }
                "heart-o" -> {
                    val p = GeneralPath()
                    p.moveTo(s * 0.50, s * 0.76)
                    p.curveTo(s * 0.12, s * 0.50, s * 0.20, s * 0.18, s * 0.50, s * 0.36)
                    p.curveTo(s * 0.80, s * 0.18, s * 0.88, s * 0.50, s * 0.50, s * 0.76)
                    p.closePath()
                    g.draw(p)
                }
                "search" -> {
                    g.draw(Ellipse2D.Double(s * 0.20, s * 0.20, s * 0.42, s * 0.42))
                    g.draw(java.awt.geom.Line2D.Double(s * 0.58, s * 0.58, s * 0.80, s * 0.80))
                }
                "home" -> {
                    val p = GeneralPath()
                    p.moveTo(s * 0.18, s * 0.50); p.lineTo(s * 0.50, s * 0.20); p.lineTo(s * 0.82, s * 0.50)
                    g.draw(p)
                    g.draw(RoundRectangle2D.Double(s * 0.28, s * 0.48, s * 0.44, s * 0.34, s * 0.06, s * 0.06))
                }
                "queue" -> {
                    val sw = (s * 0.085f).toFloat()
                    listOf(0.30, 0.50, 0.70).forEach { yy -> g.draw(java.awt.geom.Line2D.Double(s * 0.20, s * yy, s * 0.66, s * yy)) }
                    val p = GeneralPath(); p.moveTo(s * 0.74, s * 0.58); p.lineTo(s * 0.90, s * 0.70); p.lineTo(s * 0.74, s * 0.82); p.closePath(); g.fill(p)
                }
                "wave" -> { // equalizer logo / now playing
                    val xs = doubleArrayOf(0.24, 0.40, 0.56, 0.72)
                    val hs = doubleArrayOf(0.34, 0.60, 0.46, 0.70)
                    xs.forEachIndexed { i, xx ->
                        val h = hs[i] * s; val w = s * 0.10
                        g.fill(RoundRectangle2D.Double(s * xx, (s - h) / 2 + s * 0.05, w, h, w * 0.5, w * 0.5))
                    }
                }
                "lyrics" -> {
                    g.draw(RoundRectangle2D.Double(s * 0.16, s * 0.20, s * 0.68, s * 0.48, s * 0.12, s * 0.12))
                    val p = GeneralPath(); p.moveTo(s * 0.30, s * 0.68); p.lineTo(s * 0.30, s * 0.82); p.lineTo(s * 0.44, s * 0.68); g.fill(p)
                    listOf(0.34 to 0.62, 0.40 to 0.50).forEach { (yy, w) ->
                        g.draw(java.awt.geom.Line2D.Double(s * 0.26, s * yy, s * (0.26 + w * 0.5), s * yy))
                    }
                }
                "note" -> {
                    g.fill(Ellipse2D.Double(s * 0.26, s * 0.60, s * 0.20, s * 0.16))
                    g.fill(RoundRectangle2D.Double(s * 0.44, s * 0.24, s * 0.055, s * 0.44, s * 0.03, s * 0.03))
                    val p = GeneralPath(); p.moveTo(s * 0.49, s * 0.24); p.quadTo(s * 0.70, s * 0.28, s * 0.66, s * 0.44); p.quadTo(s * 0.62, s * 0.34, s * 0.49, s * 0.34); p.closePath(); g.fill(p)
                }
                "gear" -> {
                    g.draw(Ellipse2D.Double(s * 0.34, s * 0.34, s * 0.32, s * 0.32))
                    for (i in 0 until 8) {
                        val a = Math.toRadians(i * 45.0)
                        val cx = s * 0.5; val cy = s * 0.5
                        g.draw(java.awt.geom.Line2D.Double(cx + Math.cos(a) * s * 0.30, cy + Math.sin(a) * s * 0.30, cx + Math.cos(a) * s * 0.42, cy + Math.sin(a) * s * 0.42))
                    }
                }
                "volume" -> {
                    val p = GeneralPath()
                    p.moveTo(s * 0.16, s * 0.38); p.lineTo(s * 0.30, s * 0.38); p.lineTo(s * 0.46, s * 0.24)
                    p.lineTo(s * 0.46, s * 0.76); p.lineTo(s * 0.30, s * 0.62); p.lineTo(s * 0.16, s * 0.62); p.closePath()
                    g.fill(p)
                    g.draw(java.awt.geom.Arc2D.Double(s * 0.42, s * 0.30, s * 0.30, s * 0.40, -55.0, 110.0, java.awt.geom.Arc2D.OPEN))
                }
                "chevron" -> {
                    val p = GeneralPath(); p.moveTo(s * 0.40, s * 0.28); p.lineTo(s * 0.62, s * 0.50); p.lineTo(s * 0.40, s * 0.72); g.draw(p)
                }
                "download" -> {
                    g.draw(java.awt.geom.Line2D.Double(s * 0.5, s * 0.18, s * 0.5, s * 0.60))
                    val a = GeneralPath(); a.moveTo(s * 0.32, s * 0.44); a.lineTo(s * 0.5, s * 0.64); a.lineTo(s * 0.68, s * 0.44); g.draw(a)
                    g.draw(java.awt.geom.Line2D.Double(s * 0.24, s * 0.80, s * 0.76, s * 0.80))
                }
                "trash" -> {
                    g.draw(java.awt.geom.Line2D.Double(s * 0.22, s * 0.30, s * 0.78, s * 0.30))
                    g.draw(java.awt.geom.Line2D.Double(s * 0.40, s * 0.30, s * 0.42, s * 0.20))
                    g.draw(java.awt.geom.Line2D.Double(s * 0.42, s * 0.20, s * 0.58, s * 0.20))
                    g.draw(java.awt.geom.Line2D.Double(s * 0.58, s * 0.20, s * 0.60, s * 0.30))
                    val p = GeneralPath(); p.moveTo(s * 0.30, s * 0.32); p.lineTo(s * 0.34, s * 0.80); p.lineTo(s * 0.66, s * 0.80); p.lineTo(s * 0.70, s * 0.32); g.draw(p)
                    listOf(0.43, 0.5, 0.57).forEach { x -> g.draw(java.awt.geom.Line2D.Double(s * x, s * 0.40, s * x, s * 0.72)) }
                }
                "check" -> {
                    val p = GeneralPath(); p.moveTo(s * 0.22, s * 0.52); p.lineTo(s * 0.42, s * 0.72); p.lineTo(s * 0.78, s * 0.30); g.draw(p)
                }
                "expand" -> {
                    val a = s * 0.20; val b = s * 0.80; val L = s * 0.20
                    // four corner brackets
                    g.draw(java.awt.geom.Line2D.Double(a, a + L, a, a)); g.draw(java.awt.geom.Line2D.Double(a, a, a + L, a))
                    g.draw(java.awt.geom.Line2D.Double(b - L, a, b, a)); g.draw(java.awt.geom.Line2D.Double(b, a, b, a + L))
                    g.draw(java.awt.geom.Line2D.Double(a, b - L, a, b)); g.draw(java.awt.geom.Line2D.Double(a, b, a + L, b))
                    g.draw(java.awt.geom.Line2D.Double(b, b - L, b, b)); g.draw(java.awt.geom.Line2D.Double(b - L, b, b, b))
                }
                "close" -> {
                    g.draw(java.awt.geom.Line2D.Double(s * 0.30, s * 0.30, s * 0.70, s * 0.70))
                    g.draw(java.awt.geom.Line2D.Double(s * 0.70, s * 0.30, s * 0.30, s * 0.70))
                }
            }
        } finally {
            g.dispose()
        }
    }

    private fun arrowedLine(x1: Double, y1: Double, x2: Double, y2: Double, s: Double): java.awt.Shape {
        val path = GeneralPath()
        val w = s * 0.075
        val dx = x2 - x1; val dy = y2 - y1; val len = Math.hypot(dx, dy)
        val nx = -dy / len * w; val ny = dx / len * w
        path.moveTo(x1 + nx, y1 + ny); path.lineTo(x2 + nx, y2 + ny); path.lineTo(x2 - nx, y2 - ny); path.lineTo(x1 - nx, y1 - ny); path.closePath()
        // arrowhead
        val ah = s * 0.16
        val ang = Math.atan2(dy, dx)
        path.moveTo(x2, y2)
        path.lineTo(x2 - ah * Math.cos(ang - 0.5), y2 - ah * Math.sin(ang - 0.5))
        path.lineTo(x2 - ah * Math.cos(ang + 0.5), y2 - ah * Math.sin(ang + 0.5))
        path.closePath()
        return path
    }
}
