package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.Houses

/** Mistake queries shared by the reducer, the UI mapper and the hint engine (§7.3). */
object MistakeChecker {

    /** Cells whose value repeats in one of their houses; both duplicates are reported, givens included. */
    fun conflicts(spec: GridSpec, values: IntArray): Set<Int> = buildSet {
        val peers = Houses.of(spec).peers
        for (cell in values.indices) {
            val v = values[cell]
            if (v != 0 && peers[cell].any { values[it] == v }) add(cell)
        }
    }

    /** Filled non-given cells whose value differs from the solution. */
    fun wrongCells(session: GameSession): Set<Int> = buildSet {
        for (cell in session.values.indices) {
            val v = session.values[cell]
            if (v != 0 && !session.isGiven(cell) && v != session.solution[cell]) add(cell)
        }
    }

    /** Empty cells that have notes, but not the cell's solution digit. */
    fun wrongNoteCells(session: GameSession): Set<Int> = buildSet {
        for (cell in session.notes.indices) {
            val notes = Candidates(session.notes[cell])
            if (session.values[cell] == 0 && !notes.isEmpty && session.solution[cell] !in notes) add(cell)
        }
    }
}
