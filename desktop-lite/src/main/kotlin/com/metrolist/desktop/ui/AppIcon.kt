package com.metrolist.desktop.ui

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** The bundled app icon (transparent, rounded). Loaded once from module resources. */
object AppIcon {
    val image: BufferedImage? by lazy {
        runCatching { AppIcon::class.java.getResourceAsStream("/icon-512.png")?.use { ImageIO.read(it) } }.getOrNull()
    }
}
