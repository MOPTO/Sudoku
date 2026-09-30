package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.TestGrids.respectsGivens
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleDiggerTest {

    private val cases = listOf(
        GridSpec.Mini4 to DifficultyProfile(4, 7),
        GridSpec.Six to DifficultyProfile(9, 13),
        GridSpec.Classic to DifficultyProfile(26, 31),
    )

    @Test
    fun digsUniquePuzzlesWithinBounds() {
        for ((spec, profile) in cases) {
            for (symmetric in listOf(true, false)) {
                for (seed in 0L until 30L) {
                    val random = Random(seed)
                    val solution = SolutionGenerator.generate(spec, random)
                    val givens = PuzzleDigger.dig(spec, solution, profile, random, symmetric)
                    val label = "${spec.size}x${spec.size} symmetric=$symmetric seed=$seed"

                    assertTrue(respectsGivens(givens, solution), label)
                    assertEquals(1, BruteForceSolver.countSolutions(Grid.ofGivens(spec, givens)), label)
                    assertTrue(givens.count { it != 0 } >= profile.minClues, label)
                    if (symmetric) assertTrue(isSymmetric(givens), label)
                }
            }
        }
    }

    @Test
    fun sameRandomSameGivens() {
        for ((spec, profile) in cases) {
            val solution = SolutionGenerator.generate(spec, Random(1))
            assertContentEquals(
                PuzzleDigger.dig(spec, solution, profile, Random(7), symmetric = true),
                PuzzleDigger.dig(spec, solution, profile, Random(7), symmetric = true),
            )
        }
    }

    @Test
    fun callsCheckpointWhileDigging() {
        var calls = 0
        val solution = SolutionGenerator.generate(GridSpec.Classic, Random(3))
        PuzzleDigger.dig(GridSpec.Classic, solution, DifficultyProfile(36, 45), Random(3), true) { calls++ }
        assertTrue(calls > 0)
    }

    private fun isSymmetric(givens: IntArray): Boolean =
        givens.indices.all { (givens[it] == 0) == (givens[givens.size - 1 - it] == 0) }
}
