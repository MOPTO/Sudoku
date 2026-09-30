package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PuzzleGeneratorBehaviourTest {

    @Test
    fun fallsBackToClosestAttemptWhenBudgetRunsOut() = runBlocking {
        // 17 clues is essentially never reached by a single random dig.
        val impossible = DifficultyProfile(17, 17)
        val config = GenerationConfig(maxAttempts = 3)
        val result = PuzzleGenerator.generate(GridSpec.Classic, Difficulty.Expert, impossible, seed = 5, config)

        assertFalse(result.inRange)
        assertEquals(3, result.attempts)
        assertEquals(1, BruteForceSolver.countSolutions(result.puzzle.givens))

        val chain = generateSequence(5L) { splitMix64(it) }.take(3).toList()
        val clues = chain.map {
            PuzzleGenerator.runAttempt(GridSpec.Classic, Difficulty.Expert, impossible, it, config).puzzle.clueCount
        }
        assertEquals(clues.min(), result.puzzle.clueCount)
        assertTrue(result.puzzle.seed in chain)
    }

    @Test
    fun cancellationStopsGeneration() = runBlocking {
        var finished = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            PuzzleGenerator.generate(GridSpec.Classic, Difficulty.Expert, seed = 1)
            finished = true
        }
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(finished)
    }

    @Test
    fun unofferedPairIsRejected() {
        assertFailsWith<IllegalArgumentException> { PuzzleGenerator.attempt(GridSpec.Mini4, Difficulty.Hard, 1) }
        assertFailsWith<IllegalArgumentException> {
            runBlocking { PuzzleGenerator.generate(GridSpec.Six, Difficulty.Expert, 1) }
        }
    }

    @Test
    fun seedChainProducesDistinctSeeds() {
        val chain = generateSequence(0L) { splitMix64(it) }.take(1000).toList()
        assertEquals(chain.size, chain.toSet().size)
        assertNotEquals(0L, splitMix64(0L))
    }
}
