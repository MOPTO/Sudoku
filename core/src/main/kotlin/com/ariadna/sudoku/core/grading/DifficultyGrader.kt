package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.logic.LogicalSolver
import com.ariadna.sudoku.core.logic.Tier
import com.ariadna.sudoku.core.model.Grid

/**
 * Difficulty of a puzzle (§4.5). [tier] is the hardest technique the logical solver needed; if it got
 * stuck ([solved] false), [tier] is a lower bound: the first tier above the implemented ones.
 * [score] sums the technique base cost of every step (internal, for tuning only).
 */
data class Grade(val tier: Tier, val score: Int, val solved: Boolean)

object DifficultyGrader {

    fun grade(grid: Grid): Grade {
        val result = LogicalSolver().solve(grid)
        val score = result.steps.sumOf { it.technique.baseCost }
        val tier = if (result.solved) result.hardestTier else Tier.entries[LogicalSolver.MAX_TIER.ordinal + 1]
        return Grade(tier, score, result.solved)
    }
}
