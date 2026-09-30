package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.TestGrids
import com.ariadna.sudoku.core.logic.LogicalSolver
import com.ariadna.sudoku.core.logic.Tier
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyGraderTest {

    @Test
    fun fullGridNeedsNothing() {
        assertEquals(Grade(Tier.T1, 0, solved = true), DifficultyGrader.grade(Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_SOLUTION)))
    }

    @Test
    fun scoreSumsTheStepCosts() {
        val grid = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        val steps = LogicalSolver().solve(grid).steps
        val grade = DifficultyGrader.grade(grid)
        assertEquals(steps.sumOf { it.technique.baseCost }, grade.score)
        assertEquals(steps.maxOf { it.technique.tier }, grade.tier)
        assertEquals(true, grade.solved)
    }

    @Test
    fun stuckPuzzleGradesAboveTheImplementedTiers() {
        val grid = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_BEYOND_T3)
        assertEquals(1, BruteForceSolver.countSolutions(grid))
        val grade = DifficultyGrader.grade(grid)
        assertEquals(false, grade.solved)
        assertEquals(Tier.entries[LogicalSolver.MAX_TIER.ordinal + 1], grade.tier)
    }
}
