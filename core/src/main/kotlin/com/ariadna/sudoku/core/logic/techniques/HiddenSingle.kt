package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Placement
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.HouseKind

/** A digit that fits in only one cell of a house goes there. Boxes are searched first, then rows and columns. */
internal object HiddenSingle : TechniqueFinder {
    override val technique = Technique.HiddenSingle

    override fun find(board: LogicBoard): Step? {
        val houses = board.houses
        for (pass in 0..1) {
            for (h in houses.all.indices) {
                if ((houses.kind(h) == HouseKind.Box) != (pass == 0)) continue
                val house = houses.all[h]
                var once = 0
                var twice = 0
                for (cell in house) {
                    val c = board.candidateBits(cell)
                    twice = twice or (once and c)
                    once = once or c
                }
                val hidden = once and twice.inv()
                if (hidden == 0) continue
                val digit = hidden.countTrailingZeroBits() + 1
                val cell = house.first { digit in board.candidates(it) }
                return Step(technique, h, listOf(cell), Candidates.of(digit), placement = Placement(cell, digit))
            }
        }
        return null
    }
}
