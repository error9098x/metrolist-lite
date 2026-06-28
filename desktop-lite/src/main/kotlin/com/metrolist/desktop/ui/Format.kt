package com.metrolist.desktop.ui

/** Seconds -> m:ss, clamped to 0. */
fun mmss(seconds: Double): String {
    val s = seconds.coerceAtLeast(0.0).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
