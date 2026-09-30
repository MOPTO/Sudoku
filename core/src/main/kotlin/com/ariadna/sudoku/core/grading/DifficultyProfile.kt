package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.logic.LogicalSolver
import com.ariadna.sudoku.core.logic.Tier
import com.ariadna.sudoku.core.model.GridSpec

/**
 * Generation bounds for one (spec, difficulty) pair. Clue counts are bounds for the digger; the
 * technique [tiers] (and [minScore]) define the difficulty. The defaults accept any tier.
 */
data class DifficultyProfile(
    val minClues: Int,
    val maxClues: Int,
    val tiers: ClosedRange<Tier> = Tier.T1..Tier.T7,
    val minScore: Int = 0,
) {
    init {
        require(minClues in 1..maxClues) { "invalid clue range $minClues..$maxClues" }
        require(!tiers.isEmpty()) { "invalid tier range $tiers" }
        require(minScore >= 0) { "minScore must be >= 0" }
    }

    /**
     * Highest tier the digger lets the puzzle need, or null when the range reaches above the implemented
     * techniques (the logical solver cannot check such a ceiling).
     */
    val ceiling: Tier? get() = tiers.endInclusive.takeIf { it <= LogicalSolver.MAX_TIER }

    /** Floor check: true if a puzzle with [grade] belongs to this profile. */
    fun accepts(grade: Grade): Boolean = grade.tier in tiers && grade.score >= minScore

    companion object {
        private val T1 = Tier.T1..Tier.T1
        private val UpToT2 = Tier.T1..Tier.T2
        private val T2 = Tier.T2..Tier.T2
        private val T3 = Tier.T3..Tier.T3
        private val FromT3 = Tier.T3..Tier.T7

        // Interim until T4 exists (milestone 11): "the T1–T3 solver gets stuck". Milestone 11 tightens
        // Hard to T4..T4 and Expert to T5..T7.
        private val BeyondT3 = Tier.T4..Tier.T7

        // §4.6 difficulty ladder. Tune here after play-testing. Small grids need fewer techniques
        // (every unique 4×4 puzzle falls to T1, T3 is rare on 6×6), so their ladder sits lower.
        private val table: Map<GridSpec, Map<Difficulty, DifficultyProfile>> = mapOf(
            GridSpec.Mini4 to mapOf(
                Difficulty.Easy to DifficultyProfile(6, 10, T1),
                Difficulty.Medium to DifficultyProfile(4, 7, T1),
            ),
            GridSpec.Six to mapOf(
                Difficulty.Easy to DifficultyProfile(16, 24, UpToT2),
                Difficulty.Medium to DifficultyProfile(10, 14, T2),
                Difficulty.Hard to DifficultyProfile(9, 12, FromT3),
            ),
            GridSpec.Classic to mapOf(
                Difficulty.Easy to DifficultyProfile(36, 45, UpToT2),
                Difficulty.Medium to DifficultyProfile(30, 37, T3),
                Difficulty.Hard to DifficultyProfile(26, 31, BeyondT3),
                Difficulty.Expert to DifficultyProfile(22, 27, BeyondT3),
            ),
        )

        /** Profile for [spec] and [difficulty], or null if that pair is not offered. */
        fun of(spec: GridSpec, difficulty: Difficulty): DifficultyProfile? = table[spec]?.get(difficulty)

        /** Difficulties offered for [spec], easiest first (empty if the size is not offered). */
        fun supported(spec: GridSpec): List<Difficulty> = table[spec]?.keys?.sorted().orEmpty()
    }
}
