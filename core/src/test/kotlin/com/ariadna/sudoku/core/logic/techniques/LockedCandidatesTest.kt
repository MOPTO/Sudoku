package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.LogicTestBoards
import com.ariadna.sudoku.core.logic.LogicTestBoards.eliminations
import com.ariadna.sudoku.core.logic.LogicTestBoards.remove
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LockedCandidatesTest {

    @Test
    fun pointingRemovesTheDigitFromTheLineOutsideTheBox() {
        // In box 0 (house 18), 5 fits only in row 0.
        val board = LogicTestBoards.empty().remove(listOf(9, 10, 11, 18, 19, 20), 5)
        assertEquals(
            Step(Technique.LockedPointing, 18, listOf(0, 1, 2), Candidates.of(5), eliminations = eliminations(3..8, 5)),
            LockedCandidates.Pointing.find(board),
        )
    }

    @Test
    fun pointingAlongTheShortSideOfASixBySixBox() {
        // 6×6 box 0 (house 12) covers cells 0,1,2 / 6,7,8; 4 fits only in column 1.
        val board = LogicTestBoards.empty(GridSpec.Six).remove(listOf(0, 2, 6, 8), 4)
        assertEquals(
            Step(
                Technique.LockedPointing, 12, listOf(1, 7), Candidates.of(4),
                eliminations = eliminations(listOf(13, 19, 25, 31), 4),
            ),
            LockedCandidates.Pointing.find(board),
        )
    }

    @Test
    fun claimingRemovesTheDigitFromTheBoxOutsideTheLine() {
        // In row 0 (house 0), 6 fits only in box 0.
        val board = LogicTestBoards.empty().remove(3..8, 6)
        assertEquals(
            Step(
                Technique.LockedClaiming, 0, listOf(0, 1, 2), Candidates.of(6),
                eliminations = eliminations(listOf(9, 10, 11, 18, 19, 20), 6),
            ),
            LockedCandidates.Claiming.find(board),
        )
    }

    @Test
    fun claimingOnASixBySixGrid() {
        // In row 0, 2 fits only in box 1 (cells 3,4,5 / 9,10,11).
        val board = LogicTestBoards.empty(GridSpec.Six).remove(0..2, 2)
        assertEquals(
            Step(Technique.LockedClaiming, 0, listOf(3, 4, 5), Candidates.of(2), eliminations = eliminations(9..11, 2)),
            LockedCandidates.Claiming.find(board),
        )
    }

    @Test
    fun noStepWithoutEliminations() {
        assertNull(LockedCandidates.Pointing.find(LogicTestBoards.empty()))
        assertNull(LockedCandidates.Claiming.find(LogicTestBoards.empty()))
        // 5 is locked to row 0 in box 0, but the rest of row 0 already lacks it.
        val board = LogicTestBoards.empty().remove(listOf(9, 10, 11, 18, 19, 20), 5).remove(3..8, 5)
        assertNull(LockedCandidates.Pointing.find(board))
    }
}
