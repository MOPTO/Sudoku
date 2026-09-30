package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.grading.DifficultyGrader
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
            if (best == null || attempt.miss < best.miss) best = attempt
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
        val grid = Grid.ofGivens(spec, givens)
        val grade = DifficultyGrader.grade(grid)
        val puzzle = Puzzle(spec, difficulty, seed, grid, solution, grade)
        // Floor check: tier range, clue range and minimum score.
        val clues = puzzle.clueCount
        val miss = Miss(
            tierGap = gap(grade.tier.ordinal, profile.tiers.start.ordinal, profile.tiers.endInclusive.ordinal),
            clueGap = gap(clues, profile.minClues, profile.maxClues),
            scoreGap = maxOf(0, profile.minScore - grade.score),
        )
        return Attempt(puzzle, miss)
    }

    /** How far [value] lies outside [min]..[max] (0 = inside). */
    private fun gap(value: Int, min: Int, max: Int): Int = maxOf(0, value - max) + maxOf(0, min - value)

    private fun profileOf(spec: GridSpec, difficulty: Difficulty): DifficultyProfile =
        requireNotNull(DifficultyProfile.of(spec, difficulty)) {
            "$difficulty is not offered for ${spec.size}x${spec.size}"
        }

    /** How far an attempt misses its profile. Compared tier first, then clues, then score. */
    internal data class Miss(val tierGap: Int, val clueGap: Int, val scoreGap: Int) : Comparable<Miss> {
        val isZero: Boolean get() = tierGap == 0 && clueGap == 0 && scoreGap == 0

        override fun compareTo(other: Miss): Int = compareValuesBy(this, other, { it.tierGap }, { it.clueGap }, { it.scoreGap })
    }

    /** One attempt's puzzle and how far it lies outside the profile. */
    internal class Attempt(val puzzle: Puzzle, val miss: Miss) {
        val inRange: Boolean get() = miss.isZero
    }
}
