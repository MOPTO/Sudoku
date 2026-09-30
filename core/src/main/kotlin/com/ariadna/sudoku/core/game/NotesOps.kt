package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.Houses

/** Pencil-mark helpers. They only compute [CellChange]s; the reducer applies them. */
object NotesOps {

    /** Digits no filled peer of [cell] blocks, given the player's [values] (right or wrong). */
    fun candidates(spec: GridSpec, values: IntArray, cell: Int): Candidates {
        var used = 0
        for (peer in Houses.of(spec).peers[cell]) if (values[peer] != 0) used = used or Candidates.bit(values[peer])
        return Candidates(Candidates.all(spec.size).bits and used.inv())
    }

    /** Auto-notes: every empty cell gets its [candidates]. Only cells whose notes change are returned. */
    fun fillAll(spec: GridSpec, values: IntArray, notes: IntArray): List<CellChange> = buildList {
        for (cell in values.indices) {
            if (values[cell] != 0) continue
            val filled = candidates(spec, values, cell).bits
            if (filled != notes[cell]) add(CellChange(cell, 0, 0, notes[cell], filled))
        }
    }

    /** Removes [digit] from the notes of every peer of [cell] that has it. */
    fun removeFromPeers(spec: GridSpec, values: IntArray, notes: IntArray, cell: Int, digit: Int): List<CellChange> {
        val clear = Candidates.bit(digit).inv()
        return buildList {
            for (peer in Houses.of(spec).peers[cell]) {
                if (digit in Candidates(notes[peer])) {
                    add(CellChange(peer, values[peer], values[peer], notes[peer], notes[peer] and clear))
                }
            }
        }
    }
}
