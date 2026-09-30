package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.logic.techniques.FullHouse
import com.ariadna.sudoku.core.logic.techniques.HiddenPair
import com.ariadna.sudoku.core.logic.techniques.HiddenSingle
import com.ariadna.sudoku.core.logic.techniques.LockedCandidates
import com.ariadna.sudoku.core.logic.techniques.NakedPair
import com.ariadna.sudoku.core.logic.techniques.NakedSingle
import com.ariadna.sudoku.core.model.Grid

/** Outcome of [LogicalSolver.solve]: the steps taken, in order, and whether they filled the grid. */
class LogicalResult(val steps: List<Step>, val solved: Boolean) {
    /** Tier of the hardest technique used ([Tier.T1] if no step was needed). */
    val hardestTier: Tier get() = steps.maxOfOrNull { it.technique.tier } ?: Tier.T1
}

/**
 * Solves like a human (§4.5): repeatedly applies the easiest applicable technique with a tier up to
 * [maxTier], restarting from the easiest after every step. Input grids must have at least one solution.
 *
 * Every step is a sound deduction, so reaching a full grid proves the puzzle has exactly one solution.
 */
class LogicalSolver(val maxTier: Tier = MAX_TIER) {

    private val finders: List<TechniqueFinder> = ALL_FINDERS.filter { it.technique.tier <= maxTier }

    /** The easiest step available on [board], or null if no allowed technique applies. */
    fun nextStep(board: LogicBoard): Step? {
        for (finder in finders) finder.find(board)?.let { return it }
        return null
    }

    fun solve(grid: Grid): LogicalResult {
        val board = LogicBoard.of(grid)
        val steps = ArrayList<Step>()
        while (!board.isSolved) {
            val step = nextStep(board) ?: break
            board.apply(step)
            steps += step
        }
        return LogicalResult(steps, board.isSolved)
    }

    fun solves(grid: Grid): Boolean = solve(grid).solved

    companion object {
        /** Highest tier with implemented techniques. */
        val MAX_TIER = Tier.T3

        private val ALL_FINDERS: List<TechniqueFinder> = listOf(
            FullHouse,
            NakedSingle,
            HiddenSingle,
            LockedCandidates.Pointing,
            LockedCandidates.Claiming,
            NakedPair,
            HiddenPair,
        ).also { finders -> check(finders.map { it.technique } == Technique.entries) }
    }
}
