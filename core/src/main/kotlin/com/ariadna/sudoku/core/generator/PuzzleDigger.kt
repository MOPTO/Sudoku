package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.DifficultyGrader
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.logic.LogicalSolver
import com.ariadna.sudoku.core.logic.Tier
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlin.random.Random

/**
 * Removes clues from a full solution while the puzzle keeps exactly one solution and stays within the
 * profile's tier ceiling (§4.4).
 */
internal object PuzzleDigger {

    /**
     * Returns the givens (0 = empty) left after digging [solution]. Digging aims for a clue count drawn
     * from the profile's range and never goes below [DifficultyProfile.minClues]. If the puzzle is still
     * easier than the profile's tier floor at the target, it keeps digging towards `minClues` until the
     * floor is met. It may stop above the target when no further removal keeps the solution unique and
     * within the ceiling. [checkpoint] runs once per candidate removal (cancellation hook).
     */
    fun dig(
        spec: GridSpec,
        solution: IntArray,
        profile: DifficultyProfile,
        random: Random,
        symmetric: Boolean,
        checkpoint: () -> Unit = {},
    ): IntArray {
        val target = random.nextInt(profile.minClues, profile.maxClues + 1)
        val last = spec.cellCount - 1
        val order = (0..last).filter { !symmetric || it <= last - it }.shuffled(random)
        val givens = solution.copyOf()
        var count = spec.cellCount
        val ceilingSolver = profile.ceiling?.let { LogicalSolver(it) }
        var tier: Tier? = null   // tier of the current givens, when known

        for (cell in order) {
            checkpoint()
            if (count <= target) {
                val current = tier ?: DifficultyGrader.grade(Grid.ofGivens(spec, givens)).tier
                tier = current
                if (current >= profile.tiers.start) break
            }
            val mirror = last - cell
            val removed = if (symmetric && mirror != cell) 2 else 1
            if (count - removed < profile.minClues) continue

            val candidate = givens.copyOf()
            candidate[cell] = 0
            if (removed == 2) candidate[mirror] = 0
            val grid = Grid.ofGivens(spec, candidate)
            // Ceiling check. A logical solve also proves uniqueness, so the brute-force count is only
            // needed when there is no ceiling to check.
            if (ceilingSolver != null) {
                val result = ceilingSolver.solve(grid)
                if (!result.solved) continue
                tier = result.hardestTier
            } else {
                if (BruteForceSolver.countSolutions(grid, limit = 2) != 1) continue
                tier = null
            }

            candidate.copyInto(givens)
            count -= removed
        }
        return givens
    }
}
