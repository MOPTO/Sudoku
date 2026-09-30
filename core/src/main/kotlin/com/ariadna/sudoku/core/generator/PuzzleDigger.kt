package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlin.random.Random

/** Removes clues from a full solution while the puzzle keeps exactly one solution (§4.4). */
internal object PuzzleDigger {

    /**
     * Returns the givens (0 = empty) left after digging [solution]. Digging aims for a clue count drawn
     * from the profile's range, never goes below [DifficultyProfile.minClues], and may stop above the
     * target when no further removal keeps the solution unique. [checkpoint] runs once per candidate
     * removal (cancellation hook).
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

        for (cell in order) {
            checkpoint()
            if (count <= target) break
            val mirror = last - cell
            val removed = if (symmetric && mirror != cell) 2 else 1
            if (count - removed < profile.minClues) continue

            val candidate = givens.copyOf()
            candidate[cell] = 0
            if (removed == 2) candidate[mirror] = 0
            if (BruteForceSolver.countSolutions(Grid.ofGivens(spec, candidate), limit = 2) != 1) continue
            // Milestone 4: ceiling check (logical solver limited to the profile's max tier) goes here.

            candidate.copyInto(givens)
            count -= removed
        }
        return givens
    }
}
