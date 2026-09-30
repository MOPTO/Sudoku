package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Placement
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates

/** The last empty cell of a house takes the one digit the house is missing. */
internal object FullHouse : TechniqueFinder {
    override val technique = Technique.FullHouse

    override fun find(board: LogicBoard): Step? {
        val full = Candidates.all(board.spec.size).bits
        board.houses.all.forEachIndexed { h, house ->
            var empty = -1
            var emptyCount = 0
            var placed = 0
            for (cell in house) {
                val v = board.value(cell)
                if (v == 0) {
                    empty = cell
                    emptyCount++
                } else {
                    placed = placed or Candidates.bit(v)
                }
            }
            if (emptyCount != 1) return@forEachIndexed
            val digit = Candidates(full and placed.inv()).single
            if (digit != 0 && digit in board.candidates(empty)) {
                return Step(technique, h, listOf(empty), Candidates.of(digit), placement = Placement(empty, digit))
            }
        }
        return null
    }
}
