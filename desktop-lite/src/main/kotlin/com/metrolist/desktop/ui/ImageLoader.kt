package com.metrolist.desktop.ui

import com.metrolist.desktop.ui.theme.Theme
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/**
 * Async album-art loader: fetches thumbnails off the EDT, scales + rounds them, and caches by
 * (url,size). Callers get a callback on the EDT once the image is ready.
 */
object ImageLoader {

    private val pool = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "img-loader").apply { isDaemon = true }
    }
    private val cache = ConcurrentHashMap<String, BufferedImage>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    fun cached(url: String?, px: Int): BufferedImage? = url?.let { cache[key(it, px)] }

    /** Request [url] at [px]×[px]; [onReady] is invoked on the EDT (immediately if cached). */
    fun load(url: String?, px: Int, radius: Int, onReady: (BufferedImage) -> Unit) {
        if (url.isNullOrBlank()) return
        val k = key(url, px)
        cache[k]?.let { onReady(it); return }
        if (!inFlight.add(k)) return
        pool.submit {
            val img = runCatching { fetchAndProcess(sized(url, px), px, radius) }.getOrNull()
            inFlight.remove(k)
            if (img != null) {
                cache[k] = img
                SwingUtilities.invokeLater { onReady(img) }
            }
        }
    }

    /** Extract up to 3 dominant colors from the cover art (off-EDT), delivered on the EDT. */
    fun palette(url: String?, onReady: (List<Color>) -> Unit) {
        if (url.isNullOrBlank()) return
        pool.submit {
            val colors = runCatching { computePalette(sized(url, 96)) }.getOrNull()
            if (!colors.isNullOrEmpty()) SwingUtilities.invokeLater { onReady(colors) }
        }
    }

    private fun computePalette(url: String): List<Color> {
        val src = readImage(url) ?: return emptyList()

        val n = 28
        val small = BufferedImage(n, n, BufferedImage.TYPE_INT_RGB)
        small.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(src, 0, 0, n, n, null); dispose()
        }

        // Quantize into coarse buckets; score by frequency weighted toward saturated, mid-bright colors.
        val buckets = HashMap<Int, IntArray>() // key -> [count, sumR, sumG, sumB]
        for (y in 0 until n) for (x in 0 until n) {
            val rgb = small.getRGB(x, y)
            val r = (rgb shr 16) and 0xFF; val g = (rgb shr 8) and 0xFF; val b = rgb and 0xFF
            val key = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
            val acc = buckets.getOrPut(key) { IntArray(4) }
            acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
        }
        return buckets.values
            .map { acc ->
                val c = Color(acc[1] / acc[0], acc[2] / acc[0], acc[3] / acc[0])
                val hsb = Color.RGBtoHSB(c.red, c.green, c.blue, null)
                val sat = hsb[1]; val bri = hsb[2]
                val score = acc[0] * (0.35 + sat) * (0.4 + (1 - kotlin.math.abs(bri - 0.55)))
                c to score
            }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun key(url: String, px: Int) = "$px|$url"

    /** Bump googleusercontent / ytimg size params up to the requested resolution (network only). */
    private fun sized(url: String, px: Int): String =
        if (File(url).exists()) url else Regex("w\\d+-h\\d+").replace(url, "w$px-h$px")

    /** Read an image from a local file path (offline downloads) or over HTTP. */
    private fun readImage(url: String): BufferedImage? {
        val f = File(url)
        if (f.exists()) return runCatching { ImageIO.read(f) }.getOrNull()
        val conn = URI(url).toURL().openConnection()
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        return conn.getInputStream().use { ImageIO.read(it) }
    }

    private fun fetchAndProcess(url: String, px: Int, radius: Int): BufferedImage {
        val src = readImage(url) ?: error("decode failed")

        val out = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.color = Theme.surfaceHover
        g.fill(RoundRectangle2D.Float(0f, 0f, px.toFloat(), px.toFloat(), radius.toFloat(), radius.toFloat()))
        g.composite = AlphaComposite.SrcAtop
        // center-crop the source square
        val side = minOf(src.width, src.height)
        val sx = (src.width - side) / 2
        val sy = (src.height - side) / 2
        g.drawImage(src, 0, 0, px, px, sx, sy, sx + side, sy + side, null)
        g.dispose()
        return out
    }
}
