package com.metrolist.desktop.ui

/** Callback bundle shared by the Now-Playing screen and the bottom bar so both drive one controller. */
class Controls(
    val togglePlay: () -> Unit,
    val next: () -> Unit,
    val prev: () -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeat: () -> Unit,
    /** Seek to an absolute position in seconds. */
    val seek: (Double) -> Unit,
    /** Set volume 0..100. */
    val setVolume: (Int) -> Unit,
)
