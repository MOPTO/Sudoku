package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.TestGrids.isValidSolution
import com.ariadna.sudoku.core.TestGrids.respectsGivens
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Generator invariants over many seeds × every offered (spec, difficulty) pair (§11). */
class PuzzleGeneratorPropertyTest {

    private val seedsPerSpec = mapOf(GridSpec.Mini4 to 100L, GridSpec.Six to 100L, GridSpec.Classic to 30L)

    @Test
    fun generatedPuzzlesSatisfyAllInvariants() = runBlocking {
        for ((spec, seeds) in seedsPerSpec) {
            for (difficulty in DifficultyProfile.supported(spec)) {
                val profile = DifficultyProfile.of(spec, difficulty)!!
                for (seed in 0L until seeds) {
                    val label = "${spec.size}x${spec.size} $difficulty seed=$seed"
                    val result = PuzzleGenerator.generate(spec, difficulty, seed)
                    val puzzle = result.puzzle
                    val givens = puzzle.givens.values
                    val solution = puzzle.solution

                    assertTrue(result.inRange, "$label: out of range after ${result.attempts} attempts")
                    assertEquals(spec, puzzle.spec, label)
                    assertEquals(difficulty, puzzle.difficulty, label)
                    assertEquals(GENERATOR_VERSION, puzzle.generatorVersion, label)
                    assertTrue(isValidSolution(spec, solution), "$label: invalid solution")
                    assertTrue(respectsGivens(givens, solution), "$label: givens not in solution")
                    assertEquals(1, BruteForceSolver.countSolutions(puzzle.givens), "$label: not unique")
                    assertTrue(puzzle.clueCount in profile.minClues..profile.maxClues, "$label: ${puzzle.clueCount} clues")
                    assertTrue(givens.indices.all { (givens[it] == 0) == (givens[givens.size - 1 - it] == 0) }, "$label: not symmetric")

                    // Reproducible from the recorded seed alone, and generate() is deterministic.
                    assertEquals(puzzle, PuzzleGenerator.attempt(spec, difficulty, puzzle.seed), "$label: attempt differs")
                    assertEquals(result, PuzzleGenerator.generate(spec, difficulty, seed), "$label: generate differs")
                }
            }
        }
    }
}
