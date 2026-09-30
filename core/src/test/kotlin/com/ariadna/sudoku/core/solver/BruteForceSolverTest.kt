package com.ariadna.sudoku.core.solver

import com.ariadna.sudoku.core.TestGrids
import com.ariadna.sudoku.core.TestGrids.isValidSolution
import com.ariadna.sudoku.core.TestGrids.respectsGivens
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BruteForceSolverTest {

    private val specs = listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)

    @Test
    fun solvesKnownClassicPuzzle() {
        val puzzle = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        val expected = Grid.decodeValues(GridSpec.Classic, TestGrids.CLASSIC_SOLUTION)
        assertContentEquals(expected, BruteForceSolver.solve(puzzle))
        assertEquals(1, BruteForceSolver.countSolutions(puzzle))
        assertEquals(SolveResult.Unique, BruteForceSolver.classify(puzzle))
    }

    @Test
    fun solvesHardPuzzleNeedingSearch() {
        // "World's hardest Sudoku" (Arto Inkala): singles alone make no progress.
        val puzzle = Grid.parse(
            GridSpec.Classic,
            "800000000003600000070090200050007000000045700000100030001000068008500010090000400",
        )
        val solution = assertNotNull(BruteForceSolver.solve(puzzle))
        assertTrue(isValidSolution(GridSpec.Classic, solution))
        assertTrue(respectsGivens(puzzle.values, solution))
        assertEquals(1, BruteForceSolver.countSolutions(puzzle))
    }

    @Test
    fun solutionWithOneBlankPerRowSolvesBack() {
        val solutions = mapOf(
            GridSpec.Mini4 to TestGrids.MINI_SOLUTION,
            GridSpec.Six to TestGrids.SIX_SOLUTION,
            GridSpec.Classic to TestGrids.CLASSIC_SOLUTION,
        )
        for ((spec, encoded) in solutions) {
            val solution = Grid.decodeValues(spec, encoded)
            assertTrue(isValidSolution(spec, solution), "fixture for $spec is valid")
            val puzzle = solution.copyOf().also { v -> for (r in 0 until spec.size) v[r * spec.size + r] = 0 }
            val grid = Grid.ofGivens(spec, puzzle)
            assertEquals(1, BruteForceSolver.countSolutions(grid), "unique for $spec")
            assertContentEquals(solution, BruteForceSolver.solve(grid))
        }
    }

    @Test
    fun ambiguousPuzzleReportsMultiple() {
        // Blanking a 1/2 "deadly rectangle" (rows 0 and 2, columns 0 and 1) leaves two solutions.
        val values = Grid.decodeValues(GridSpec.Mini4, TestGrids.MINI_SOLUTION)
        intArrayOf(0, 1, 8, 9).forEach { values[it] = 0 }
        val grid = Grid.ofGivens(GridSpec.Mini4, values)
        assertEquals(2, BruteForceSolver.countSolutions(grid))
        assertEquals(2, BruteForceSolver.countSolutions(grid, limit = 10))
        assertEquals(1, BruteForceSolver.countSolutions(grid, limit = 1))
        assertEquals(SolveResult.Multiple, BruteForceSolver.classify(grid))
    }

    @Test
    fun emptyGridHasManySolutions() {
        for (spec in specs) {
            assertEquals(2, BruteForceSolver.countSolutions(Grid.empty(spec)), "empty $spec")
        }
        assertEquals(288, BruteForceSolver.countSolutions(Grid.empty(GridSpec.Mini4), limit = 1000))
    }

    @Test
    fun emptyGridSolvesToValidRandomGrid() {
        for (spec in specs) {
            for (seed in 0L until 20L) {
                val solution = assertNotNull(BruteForceSolver.solve(Grid.empty(spec), Random(seed)))
                assertTrue(isValidSolution(spec, solution), "$spec seed $seed")
            }
        }
    }

    @Test
    fun randomSolveIsDeterministicPerSeed() {
        val empty = Grid.empty(GridSpec.Classic)
        val a = BruteForceSolver.solve(empty, Random(42))
        val b = BruteForceSolver.solve(empty, Random(42))
        val c = BruteForceSolver.solve(empty, Random(43))
        assertContentEquals(a, b)
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun invalidGridHasNoSolution() {
        val values = IntArray(81).also { it[0] = 1; it[5] = 1 }   // two 1s in row 0
        val grid = Grid.ofGivens(GridSpec.Classic, values)
        assertEquals(0, BruteForceSolver.countSolutions(grid))
        assertNull(BruteForceSolver.solve(grid))
        assertEquals(SolveResult.NoSolution, BruteForceSolver.classify(grid))
    }

    @Test
    fun unsolvableWithoutDirectDuplicateHasNoSolution() {
        // Row 0 misses only 9, but column 8 already holds a 9: no duplicate, yet no solution.
        val values = Grid.decodeValues(GridSpec.Classic, "123456780" + "0".repeat(72))
        values[9 * 4 + 8] = 9
        val grid = Grid.ofGivens(GridSpec.Classic, values)
        assertEquals(0, BruteForceSolver.countSolutions(grid))
        assertNull(BruteForceSolver.solve(grid))
    }
}
