package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.util.formatElapsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTimerTest {

    private var now = 1_000L
    private val timer = GameTimer(initialMs = 500, clock = { now })

    @Test
    fun `accumulates only while running`() {
        assertEquals(500L, timer.elapsedMs)
        timer.start()
        now += 2_000
        assertEquals(2_500L, timer.elapsedMs)
        timer.stop()
        now += 10_000
        assertEquals(2_500L, timer.elapsedMs)
        timer.start()
        now += 1_000
        assertEquals(3_500L, timer.elapsedMs)
    }

    @Test
    fun `double start and double stop are harmless`() {
        timer.start()
        now += 1_000
        timer.start()   // must not reset runningSince
        now += 1_000
        assertEquals(2_500L, timer.elapsedMs)
        assertTrue(timer.isRunning)
        timer.stop()
        timer.stop()
        assertFalse(timer.isRunning)
        assertEquals(2_500L, timer.elapsedMs)
    }

    @Test
    fun `formats minutes and hours`() {
        assertEquals("0:00", formatElapsed(999))
        assertEquals("1:05", formatElapsed(65_000))
        assertEquals("59:59", formatElapsed(3_599_000))
        assertEquals("1:00:00", formatElapsed(3_600_000))
        assertEquals("0:00", formatElapsed(-5))
    }
}
