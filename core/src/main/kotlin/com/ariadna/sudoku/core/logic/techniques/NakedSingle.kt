package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Placement
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates

/** A cell with only one candidate left takes it. */
internal object NakedSingle : TechniqueFinder {
    override val technique = Technique.NakedSingle

    override fun find(board: LogicBoard): Step? {
        for (cell in 0 until board.spec.cellCount) {
            if (board.value(cell) != 0) continue
            val digit = board.candidates(cell).single
            if (digit != 0) {
                return Step(technique, house = null, listOf(cell), Candidates.of(digit), placement = Placement(cell, digit))
            }
        }
        return null
    }
}
