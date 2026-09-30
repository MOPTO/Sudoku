package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.LogicTestBoards
import com.ariadna.sudoku.core.logic.LogicTestBoards.only
import com.ariadna.sudoku.core.logic.LogicTestBoards.remove
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.model.Candidates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairsTest {

    private val box0 = listOf(0, 1, 2, 9, 10, 11, 18, 19, 20)

    @Test
    fun nakedPairRemovesItsDigitsFromTheRestOfTheHouse() {
        val board = LogicTestBoards.empty().only(0, 1, 2).only(1, 1, 2)
        val expected = (2..8).flatMap { listOf(Elimination(it, 1), Elimination(it, 2)) }
        assertEquals(
            Step(Technique.NakedPair, 0, listOf(0, 1), Candidates.of(1, 2), eliminations = expected),
            NakedPair.find(board),
        )
    }

    @Test
    fun nakedPairWithNothingToRemoveIsNoStep() {
        val board = LogicTestBoards.empty().only(0, 1, 2).only(1, 1, 2)
            .remove(2..8, 1, 2)
            .remove(box0 - listOf(0, 1), 1, 2)
        assertNull(NakedPair.find(board))
        assertNull(NakedPair.find(LogicTestBoards.empty()))
    }

    @Test
    fun hiddenPairRemovesTheOtherCandidatesOfItsCells() {
        // In row 0, 3 and 8 fit only in cells 4 and 7.
        val board = LogicTestBoards.empty().remove((0..8) - listOf(4, 7), 3, 8)
        val others = listOf(1, 2, 4, 5, 6, 7, 9)
        val expected = listOf(4, 7).flatMap { cell -> others.map { Elimination(cell, it) } }
        assertEquals(
            Step(Technique.HiddenPair, 0, listOf(4, 7), Candidates.of(3, 8), eliminations = expected),
            HiddenPair.find(board),
        )
    }

    @Test
    fun hiddenPairWithNothingToRemoveIsNoStep() {
        val board = LogicTestBoards.empty().remove((0..8) - listOf(4, 7), 3, 8).only(4, 3, 8).only(7, 3, 8)
        assertNull(HiddenPair.find(board))
        assertNull(HiddenPair.find(LogicTestBoards.empty()))
    }
}
