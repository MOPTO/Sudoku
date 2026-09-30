package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.generator.PuzzleDigger
import com.ariadna.sudoku.core.generator.PuzzleGenerator
import com.ariadna.sudoku.core.generator.SolutionGenerator
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Safety net for every technique (§11, §13): over thousands of puzzles, no step may place a wrong digit
 * or remove a cell's solution digit.
 */
class LogicalSolverInvariantTest {

    private val used = mutableSetOf<Technique>()

    @Test
    fun everyStepIsConsistentWithTheSolution() = runBlocking {
        // Generated puzzles of every offered pair.
        for (spec in listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)) {
            for (difficulty in DifficultyProfile.supported(spec)) {
                for (seed in 0L until 40L) {
                    val puzzle = PuzzleGenerator.generate(spec, difficulty, seed).puzzle
                    check(puzzle.givens, puzzle.solution, "${spec.size}x${spec.size} $difficulty seed=$seed")
                }
            }
        }
        // Near-minimal digs without a ceiling, so hard boards exercise every technique.
        val minimal = mapOf(GridSpec.Mini4 to 4, GridSpec.Six to 8, GridSpec.Classic to 17)
        for ((spec, minClues) in minimal) {
            for (seed in 0L until 1000L) {
                val random = Random(seed)
                val solution = SolutionGenerator.generate(spec, random)
                val givens = PuzzleDigger.dig(spec, solution, DifficultyProfile(minClues, minClues), random, symmetric = seed % 2 == 0L)
                check(Grid.ofGivens(spec, givens), solution, "${spec.size}x${spec.size} dig seed=$seed")
            }
        }
        assertEquals(Technique.entries.toSet(), used, "every technique must be exercised")
    }

    private fun check(grid: Grid, solution: IntArray, label: String) {
        val solver = LogicalSolver()
        val board = LogicBoard.of(grid)
        while (true) {
            val step = solver.nextStep(board) ?: break
            used += step.technique
            step.placement?.let { assertEquals(solution[it.cell], it.digit, "$label: ${step.technique} placed wrong digit") }
            for (e in step.eliminations) {
                assertNotEquals(solution[e.cell], e.digit, "$label: ${step.technique} removed the solution digit")
            }
            board.apply(step)
            for (cell in solution.indices) {
                val ok = board.value(cell) == solution[cell] || (board.value(cell) == 0 && solution[cell] in board.candidates(cell))
                assertTrue(ok, "$label: cell $cell inconsistent after ${step.technique}")
            }
        }
    }
}
