package com.ariadna.sudoku.core.generator

/**
 * Version of the generation algorithm. A puzzle is reproducible from (spec, difficulty, seed, version);
 * bump this whenever a change alters the output for a seed (it also invalidates the puzzle pool).
 */
const val GENERATOR_VERSION = 1

data class GenerationConfig(
    /** Attempts before [PuzzleGenerator.generate] gives up and returns the best-so-far puzzle. */
    val maxAttempts: Int = 50,
    /** Remove clues in 180° rotation pairs for the classic symmetric look. */
    val symmetric: Boolean = true,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }
}
