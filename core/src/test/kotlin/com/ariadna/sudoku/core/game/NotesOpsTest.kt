package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotesOpsTest {

    private val session = GameFixtures.classic().session
    private val spec = GridSpec.Classic

    @Test
    fun candidatesAreTheDigitsNoPeerBlocks() {
        assertEquals(Candidates.of(1, 2, 4), NotesOps.candidates(spec, session.values, 2))
    }

    @Test
    fun fillAllOnlyTouchesEmptyCellsWhoseNotesChange() {
        val changes = NotesOps.fillAll(spec, session.values, session.notes)
        assertEquals(session.values.count { it == 0 }, changes.size)
        assertTrue(changes.all { session.values[it.cell] == 0 && it.oldValue == 0 && it.newValue == 0 })

        val notes = session.notes.copyOf().also { n -> changes.forEach { n[it.cell] = it.newNotes } }
        notes[2] = Candidates.of(1).bits
        assertEquals(listOf(CellChange(2, 0, 0, Candidates.of(1).bits, Candidates.of(1, 2, 4).bits)), NotesOps.fillAll(spec, session.values, notes))
    }

    @Test
    fun removeFromPeersOnlyReturnsPeersWithTheDigit() {
        val notes = IntArray(81).also {
            it[3] = Candidates.of(4, 6).bits   // row peer of 2
            it[10] = Candidates.of(6).bits     // box peer without 4
            it[40] = Candidates.of(4).bits     // not a peer
        }
        assertEquals(
            listOf(CellChange(3, 0, 0, Candidates.of(4, 6).bits, Candidates.of(6).bits)),
            NotesOps.removeFromPeers(spec, session.values, notes, 2, 4),
        )
    }
}
