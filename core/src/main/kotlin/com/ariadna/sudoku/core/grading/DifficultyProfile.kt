package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.model.GridSpec

/**
 * Generation bounds for one (spec, difficulty) pair. Clue counts are bounds for the digger,
 * not the definition of difficulty: milestone 4 adds the technique tier range and minimum score.
 */
data class DifficultyProfile(val minClues: Int, val maxClues: Int) {
    init {
        require(minClues in 1..maxClues) { "invalid clue range $minClues..$maxClues" }
    }

    companion object {
        // §4.6 difficulty ladder. Tune here after play-testing.
        private val table: Map<GridSpec, Map<Difficulty, DifficultyProfile>> = mapOf(
            GridSpec.Mini4 to mapOf(
                Difficulty.Easy to DifficultyProfile(6, 10),
                Difficulty.Medium to DifficultyProfile(4, 7),
            ),
            GridSpec.Six to mapOf(
                Difficulty.Easy to DifficultyProfile(16, 24),
                Difficulty.Medium to DifficultyProfile(12, 16),
                Difficulty.Hard to DifficultyProfile(9, 13),
            ),
            GridSpec.Classic to mapOf(
                Difficulty.Easy to DifficultyProfile(36, 45),
                Difficulty.Medium to DifficultyProfile(30, 37),
                Difficulty.Hard to DifficultyProfile(26, 31),
                Difficulty.Expert to DifficultyProfile(22, 27),
            ),
        )

        /** Profile for [spec] and [difficulty], or null if that pair is not offered. */
        fun of(spec: GridSpec, difficulty: Difficulty): DifficultyProfile? = table[spec]?.get(difficulty)

        /** Difficulties offered for [spec], easiest first (empty if the size is not offered). */
        fun supported(spec: GridSpec): List<Difficulty> = table[spec]?.keys?.sorted().orEmpty()
    }
}
