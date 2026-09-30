package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates

/** Two digits that fit only in the same two cells of a house own those cells: remove their other candidates. */
internal object HiddenPair : TechniqueFinder {
    override val technique = Technique.HiddenPair

    override fun find(board: LogicBoard): Step? {
        val size = board.spec.size
        board.houses.all.forEachIndexed { h, house ->
            // positions[d - 1]: bitmask over the house's slots where digit d is still a candidate.
            val positions = IntArray(size)
            house.forEachIndexed { slot, cell ->
                board.candidates(cell).forEach { positions[it - 1] = positions[it - 1] or (1 shl slot) }
            }
            for (d1 in 1..size) {
                val slots = positions[d1 - 1]
                if (slots.countOneBits() != 2) continue
                for (d2 in d1 + 1..size) {
                    if (positions[d2 - 1] != slots) continue
                    val pair = Candidates.of(d1, d2)
                    val cells = house.filterIndexed { slot, _ -> slots and (1 shl slot) != 0 }
                    val eliminations = mutableListOf<Elimination>()
                    for (cell in cells) eliminations.addEliminations(board, cell, pair.bits.inv())
                    if (eliminations.isNotEmpty()) {
                        return Step(technique, h, cells, pair, eliminations = eliminations)
                    }
                }
            }
        }
        return null
    }
}
