package com.metrolist.desktop.ui.theme

import java.awt.Color
import java.awt.Font

/**
 * Central dark theme tokens (colors, fonts, radii) so every widget pulls from one place.
 * Palette matches the Metrolist Lite mockups: near-black teal background, teal accent, coral for
 * "liked"/active emphasis.
 */
object Theme {
    // Surfaces
    val bg = Color(0x0F1418)
    val sidebar = Color(0x0B0F12)
    val surface = Color(0x161F26)
    val surfaceHover = Color(0x1E2A33)
    val surfaceActive = Color(0x16302E)
    val divider = Color(0x202C35)

    // Accents
    val accent = Color(0x35D0BE)        // teal
    val accentDim = Color(0x2AA89A)
    val coral = Color(0xF06A52)         // like / active repeat

    // Text
    val text = Color(0xECF1F3)
    val textDim = Color(0x8A9BA6)
    val textFaint = Color(0x5C6B75)
    val onAccent = Color(0x06201C)

    // Lyrics (soft pastel so it reads well over any album-art gradient)
    val lyricSung = Color(0xDCE6F2)   // already-sung words: light pastel
    val lyricFront = Color(0xFFFFFF)  // the word being sung right now: brightest "pointer"

    private val base: String = run {
        val available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toHashSet()
        listOf("SF Pro Text", "Helvetica Neue", "Inter", "Segoe UI").firstOrNull { it in available } ?: Font.SANS_SERIF
    }

    fun font(size: Float, bold: Boolean = false): Font =
        Font(base, if (bold) Font.BOLD else Font.PLAIN, size.toInt())

    const val RADIUS = 12
    const val ROW_RADIUS = 10

    /** Linear blend a→b by t in [0,1]. */
    fun mix(a: Color, b: Color, t: Double): Color {
        val u = 1 - t
        return Color(
            (a.red * u + b.red * t).toInt().coerceIn(0, 255),
            (a.green * u + b.green * t).toInt().coerceIn(0, 255),
            (a.blue * u + b.blue * t).toInt().coerceIn(0, 255),
        )
    }
}
