package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.util.ElapsedClock

/**
 * Elapsed play time of a timed game (§5.1). Real time from a monotonic clock, so it never drifts:
 * elapsed = accumulated + (now − runningSince). Not thread-safe; the ViewModel uses it on the main thread.
 */
class GameTimer(initialMs: Long, private val clock: ElapsedClock) {

    private var accumulatedMs = initialMs
    private var runningSince: Long? = null

    val isRunning: Boolean get() = runningSince != null

    val elapsedMs: Long get() = accumulatedMs + (runningSince?.let { clock.now() - it } ?: 0)

    fun start() {
        if (runningSince == null) runningSince = clock.now()
    }

    fun stop() {
        val since = runningSince ?: return
        accumulatedMs += clock.now() - since
        runningSince = null
    }
}
