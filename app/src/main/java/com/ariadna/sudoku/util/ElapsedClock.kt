package com.ariadna.sudoku.util

/** Monotonic milliseconds (`SystemClock.elapsedRealtime()` in the app); injectable so tests control time. */
fun interface ElapsedClock {
    fun now(): Long
}
