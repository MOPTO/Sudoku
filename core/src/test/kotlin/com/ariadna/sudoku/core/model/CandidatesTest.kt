package com.ariadna.sudoku.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CandidatesTest {

    @Test
    fun basicOperations() {
        val c = Candidates.of(1, 5, 9)
        assertEquals(3, c.count)
        assertTrue(5 in c)
        assertFalse(4 in c)
        assertEquals(listOf(1, 5, 9), c.toList())
        assertEquals(listOf(1, 9), (c - 5).toList())
        assertEquals(listOf(1, 4, 5, 9), (c + 4).toList())
        assertEquals(c, c + 5)
    }

    @Test
    fun single() {
        assertEquals(7, Candidates.of(7).single)
        assertEquals(0, Candidates.of(2, 3).single)
        assertEquals(0, Candidates.NONE.single)
        assertTrue(Candidates.NONE.isEmpty)
    }

    @Test
    fun setOperations() {
        val a = Candidates.of(1, 2, 3)
        val b = Candidates.of(2, 3, 4)
        assertEquals(listOf(2, 3), (a and b).toList())
        assertEquals(listOf(1, 2, 3, 4), (a or b).toList())
    }

    @Test
    fun allDigitsForEverySize() {
        assertEquals((1..9).toList(), Candidates.all(9).toList())
        assertEquals((1..16).toList(), Candidates.all(16).toList())
        assertEquals(16, Candidates.all(16).count)
    }
}
