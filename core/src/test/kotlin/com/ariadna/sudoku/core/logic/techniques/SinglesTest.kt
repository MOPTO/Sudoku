package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.LogicTestBoards
import com.ariadna.sudoku.core.logic.LogicTestBoards.only
import com.ariadna.sudoku.core.logic.LogicTestBoards.remove
import com.ariadna.sudoku.core.logic.Placement
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SinglesTest {

    private val nineEmpty = "0".repeat(81)

    @Test
    fun fullHouseFillsTheLastCellOfAHouse() {
        val board = LogicTestBoards.of(GridSpec.Classic, "123456780" + "0".repeat(72))
        assertEquals(
            Step(Technique.FullHouse, 0, listOf(8), Candidates.of(9), placement = Placement(8, 9)),
            FullHouse.find(board),
        )
    }

    @Test
    fun fullHouseWorksOnSixBySixBoxes() {
        // Box 1 of a 6×6 grid covers rows 0–1, columns 3–5 (house 12 + 1).
        val board = LogicTestBoards.of(GridSpec.Six, "000123" + "000450" + "0".repeat(24))
        assertEquals(
            Step(Technique.FullHouse, 13, listOf(11), Candidates.of(6), placement = Placement(11, 6)),
            FullHouse.find(board),
        )
    }

    @Test
    fun fullHouseNeedsExactlyOneEmptyCell() {
        assertNull(FullHouse.find(LogicTestBoards.of(GridSpec.Classic, "123456700" + "0".repeat(72))))
        assertNull(FullHouse.find(LogicTestBoards.of(GridSpec.Classic, nineEmpty)))
    }

    @Test
    fun nakedSingleTakesTheOnlyCandidate() {
        val board = LogicTestBoards.empty().only(40, 5)
        assertEquals(
            Step(Technique.NakedSingle, null, listOf(40), Candidates.of(5), placement = Placement(40, 5)),
            NakedSingle.find(board),
        )
        assertNull(NakedSingle.find(LogicTestBoards.empty()))
    }

    @Test
    fun hiddenSingleFindsTheOnlyPlaceInABox() {
        // Box 4 (house 22) keeps 7 only in its centre cell 40.
        val board = LogicTestBoards.empty().remove(box4() - 40, 7)
        assertEquals(
            Step(Technique.HiddenSingle, 22, listOf(40), Candidates.of(7), placement = Placement(40, 7)),
            HiddenSingle.find(board),
        )
        assertNull(HiddenSingle.find(LogicTestBoards.empty()))
    }

    @Test
    fun hiddenSingleSearchesBoxesBeforeLines() {
        // Row 0 (house 0) keeps 3 only in cell 0; box 8 (house 26) keeps 7 only in cell 80.
        val board = LogicTestBoards.empty()
            .remove(1..8, 3)
            .remove(listOf(60, 61, 62, 69, 70, 71, 78, 79), 7)
        assertEquals(
            Step(Technique.HiddenSingle, 26, listOf(80), Candidates.of(7), placement = Placement(80, 7)),
            HiddenSingle.find(board),
        )
    }

    private fun box4(): List<Int> = listOf(30, 31, 32, 39, 40, 41, 48, 49, 50)
}
