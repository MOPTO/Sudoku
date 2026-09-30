package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.TestGrids
import com.ariadna.sudoku.core.generator.PuzzleGenerator
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.logic.LogicTestBoards.only
import com.ariadna.sudoku.core.logic.LogicTestBoards.remove
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogicalSolverTest {

    @Test
    fun picksTheEasiestApplicableStep() {
        // A hidden pair in row 0 and a naked single in cell 80: the single wins.
        val board = LogicTestBoards.empty().remove((0..8) - listOf(4, 7), 3, 8).only(80, 5)
        assertEquals(Technique.NakedSingle, LogicalSolver().nextStep(board)?.technique)
        assertEquals(Technique.HiddenPair, LogicalSolver().nextStep(LogicTestBoards.empty().remove((0..8) - listOf(4, 7), 3, 8))?.technique)
    }

    @Test
    fun maxTierLimitsTheTechniques() {
        val board = LogicTestBoards.empty().remove((0..8) - listOf(4, 7), 3, 8)
        assertEquals(null, LogicalSolver(Tier.T2).nextStep(board))

        val medium = runBlocking { PuzzleGenerator.generate(GridSpec.Classic, Difficulty.Medium, seed = 0) }.puzzle
        assertEquals(Tier.T3, medium.grade.tier)
        assertFalse(LogicalSolver(Tier.T2).solves(medium.givens))
        assertTrue(LogicalSolver(Tier.T3).solves(medium.givens))
    }

    @Test
    fun solvesTheClassicPuzzle() {
        val grid = Grid.parse(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE)
        val result = LogicalSolver().solve(grid)
        assertTrue(result.solved)

        val board = LogicBoard.of(grid)
        result.steps.forEach { board.apply(it) }
        assertEquals(TestGrids.CLASSIC_SOLUTION, Grid.ofGivens(GridSpec.Classic, board.values()).encode())
        assertEquals(TestGrids.CLASSIC_PUZZLE, grid.encode())   // the input grid is untouched
    }

    @Test
    fun placementsClearTheDigitFromPeers() {
        val board = LogicTestBoards.empty()
        board.apply(LogicalSolver().nextStep(board.only(0, 4))!!)
        assertEquals(4, board.value(0))
        assertTrue(board.candidates(0).isEmpty)
        for (peer in board.houses.peers[0]) assertFalse(4 in board.candidates(peer), "peer $peer")
        assertContentEquals(IntArray(81) { if (it == 0) 4 else 0 }, board.values())
    }
}
