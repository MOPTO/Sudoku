package com.ariadna.sudoku.core.logic.techniques

import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.LogicBoard
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.logic.TechniqueFinder
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.HouseKind

/**
 * If every cell of house A that can hold a digit also lies in one other house B, the digit must go in
 * A ∩ B, so it is removed from the rest of B. Pointing: A is a box, B a line. Claiming: A is a line, B a box.
 */
internal class LockedCandidates private constructor(override val technique: Technique) : TechniqueFinder {

    override fun find(board: LogicBoard): Step? {
        val houses = board.houses
        val boxFirst = technique == Technique.LockedPointing
        for (a in houses.all.indices) {
            if ((houses.kind(a) == HouseKind.Box) != boxFirst) continue
            val house = houses.all[a]
            for (digit in 1..board.spec.size) {
                val cells = house.filter { digit in board.candidates(it) }
                if (cells.size < 2) continue
                for (b in houses.housesOf[cells[0]]) {
                    if (b == a || cells.any { b !in houses.housesOf[it] }) continue
                    val eliminations = houses.all[b]
                        .filter { a !in houses.housesOf[it] && digit in board.candidates(it) }
                        .map { Elimination(it, digit) }
                    if (eliminations.isNotEmpty()) {
                        return Step(technique, a, cells, Candidates.of(digit), eliminations = eliminations)
                    }
                }
            }
        }
        return null
    }

    companion object {
        val Pointing = LockedCandidates(Technique.LockedPointing)
        val Claiming = LockedCandidates(Technique.LockedClaiming)
    }
}
