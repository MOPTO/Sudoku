package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates

/** Two cells of a house with the same two candidates own those digits: remove them from the rest of the house. */
internal object NakedPair : TechniqueFinder {
    override val technique = Technique.NakedPair

    override fun find(board: LogicBoard): Step? {
        board.houses.all.forEachIndexed { h, house ->
            val pairs = house.filter { board.candidates(it).count == 2 }
            for (i in pairs.indices) {
                for (j in i + 1 until pairs.size) {
                    val mask = board.candidateBits(pairs[i])
                    if (board.candidateBits(pairs[j]) != mask) continue
                    val eliminations = mutableListOf<Elimination>()
                    for (cell in house) {
                        if (cell != pairs[i] && cell != pairs[j]) eliminations.addEliminations(board, cell, mask)
                    }
                    if (eliminations.isNotEmpty()) {
                        return Step(technique, h, listOf(pairs[i], pairs[j]), Candidates(mask), eliminations = eliminations)
                    }
                }
            }
        }
        return null
    }
}
