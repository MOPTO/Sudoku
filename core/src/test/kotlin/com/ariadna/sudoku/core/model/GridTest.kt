package com.ariadna.sudoku.core.model

import com.ariadna.sudoku.core.TestGrids
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GridTest {

    @Test
    fun roundTripForEverySize() {
        for (spec in listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic, GridSpec.Sixteen)) {
            val values = IntArray(spec.cellCount) { it % (spec.size + 1) }
            val grid = Grid.ofGivens(spec, values)
            val decoded = Grid.parse(spec, grid.encode())
            assertEquals(grid, decoded)
            assertContentEquals(values, decoded.values)
        }
    }

    @Test
    fun largeGridsUseLetters() {
        val values = IntArray(GridSpec.Sixteen.cellCount)
        values[0] = 10
        values[1] = 16
        values[2] = 9
        val encoded = Grid.ofGivens(GridSpec.Sixteen, values).encode()
        assertEquals("AG9", encoded.take(3))
    }

    @Test
    fun parseMarksNonEmptyCellsAsGivens() {
        val grid = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        assertTrue(grid.isGiven(0))    // 5
        assertFalse(grid.isGiven(2))   // empty
        assertEquals(30, grid.filledCount)
        assertFalse(grid.isComplete)
        assertTrue(Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_SOLUTION).isComplete)
    }

    @Test
    fun dotsAreEmpty() {
        assertEquals(Grid.parse(GridSpec.Mini4, "1..." + "0".repeat(12)), Grid.parse(GridSpec.Mini4, "1" + "0".repeat(15)))
    }

    @Test
    fun equalityIsByContent() {
        val a = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        val b = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        val sameValuesNoGivens = Grid(GridSpec.Classic, a.values, BooleanArray(81))
        assertNotEquals(a, sameValuesNoGivens)
    }

    @Test
    fun gridIsNotMutatedThroughAccessors() {
        val grid = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        grid.values[0] = 9
        assertEquals(5, grid[0])
    }

    @Test
    fun badInputIsRejected() {
        assertFailsWith<IllegalArgumentException> { Grid.parse(GridSpec.Classic, "123") }
        assertFailsWith<IllegalArgumentException> { Grid.parse(GridSpec.Classic, "A" + "0".repeat(80)) }
        assertFailsWith<IllegalArgumentException> { Grid.parse(GridSpec.Sixteen, "H" + "0".repeat(255)) }
        assertFailsWith<IllegalArgumentException> { Grid.parse(GridSpec.Mini4, "5" + "0".repeat(15)) }
    }
}
