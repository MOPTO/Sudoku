package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.model.Candidates

/** Adds an [Elimination] for every digit in the [digits] bitmask that [cell] still has as a candidate. */
internal fun MutableList<Elimination>.addEliminations(board: LogicBoard, cell: Int, digits: Int) {
    Candidates(board.candidateBits(cell) and digits).forEach { add(Elimination(cell, it)) }
}
