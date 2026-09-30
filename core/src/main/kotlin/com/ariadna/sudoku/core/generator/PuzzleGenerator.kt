package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.random.Random

/** Result of [PuzzleGenerator.generate]. [inRange] is false when the budget ran out (best-so-far puzzle). */
data class GenerationResult(val puzzle: Puzzle, val inRange: Boolean, val attempts: Int)

object PuzzleGenerator {

    /**
     * One deterministic generation attempt: solution and digging are both driven by `Random(seed)`.
     * Returns null if the result misses the difficulty profile.
     */
    fun attempt(
        spec: GridSpec,
        difficulty: Difficulty,
        seed: Long,
        config: GenerationConfig = GenerationConfig(),
    ): Puzzle? = runAttempt(spec, difficulty, profileOf(spec, difficulty), seed, config).takeIf { it.inRange }?.puzzle

    /**
     * Retries [attempt] along a seed chain (`seed`, `splitMix64(seed)`, ...) until one fits the profile.
     * The returned puzzle records the seed of its own attempt. Cancellable between and inside attempts.
     */
    suspend fun generate(
        spec: GridSpec,
        difficulty: Difficulty,
        seed: Long,
        config: GenerationConfig = GenerationConfig(),
    ): GenerationResult = generate(spec, difficulty, profileOf(spec, difficulty), seed, config)

    internal suspend fun generate(
        spec: GridSpec,
        difficulty: Difficulty,
        profile: DifficultyProfile,
        seed: Long,
        config: GenerationConfig,
    ): GenerationResult {
        val context = currentCoroutineContext()
        var attemptSeed = seed
        var best: Attempt? = null
        for (n in 1..config.maxAttempts) {
            context.ensureActive()
            val attempt = runAttempt(spec, difficulty, profile, attemptSeed, config) { context.ensureActive() }
            if (attempt.inRange) return GenerationResult(attempt.puzzle, inRange = true, attempts = n)
            if (best == null || attempt.distance < best.distance) best = attempt
            attemptSeed = splitMix64(attemptSeed)
        }
        return GenerationResult(best!!.puzzle, inRange = false, attempts = config.maxAttempts)
    }

    internal fun runAttempt(
        spec: GridSpec,
        difficulty: Difficulty,
        profile: DifficultyProfile,
        seed: Long,
        config: GenerationConfig,
        checkpoint: () -> Unit = {},
    ): Attempt {
        val random = Random(seed)
        val solution = SolutionGenerator.generate(spec, random)
        val givens = PuzzleDigger.dig(spec, solution, profile, random, config.symmetric, checkpoint)
        val puzzle = Puzzle(spec, difficulty, seed, Grid.ofGivens(spec, givens), solution)
        // Floor check. Milestone 4 adds the grader's tier range and minimum score here.
        val clues = puzzle.clueCount
        val distance = maxOf(0, clues - profile.maxClues) + maxOf(0, profile.minClues - clues)
        return Attempt(puzzle, distance)
    }

    private fun profileOf(spec: GridSpec, difficulty: Difficulty): DifficultyProfile =
        requireNotNull(DifficultyProfile.of(spec, difficulty)) {
            "$difficulty is not offered for ${spec.size}x${spec.size}"
        }

    /** One attempt's puzzle and how far its clue count lies outside the profile (0 = within it). */
    internal class Attempt(val puzzle: Puzzle, val distance: Int) {
        val inRange: Boolean get() = distance == 0
    }
}
