package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.FillAllNotes
import com.ariadna.sudoku.core.game.GameAction.Redo
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.Undo
import com.ariadna.sudoku.core.game.GameFixtures.act
import com.ariadna.sudoku.core.game.GameFixtures.enter
import com.ariadna.sudoku.core.game.GameFixtures.note
import com.ariadna.sudoku.core.game.GameFixtures.notes
import com.ariadna.sudoku.core.game.GameFixtures.value
import com.ariadna.sudoku.core.model.Candidates
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReducerHistoryTest {

    private val start = GameFixtures.classic()

    @Test
    fun undoRestoresTheValueAndEveryNoteTheMoveCleared() {
        val noted = start.note(2, 1, 4).note(3, 4, 6)
        val placed = noted.enter(2, 4)
        assertEquals(Candidates.of(6), placed.notes(3))

        val undone = placed.act(Undo)
        assertEquals(0, undone.value(2))
        assertEquals(Candidates.of(1, 4), undone.notes(2))
        assertEquals(Candidates.of(4, 6), undone.notes(3))
        assertTrue(undone.session.values.contentEquals(noted.session.values))
        assertTrue(undone.session.notes.contentEquals(noted.session.notes))

        assertEquals(placed.session, undone.act(Redo).session)
    }

    @Test
    fun aNewMoveDropsTheRedoStack() {
        val undone = start.enter(2, 4).act(Undo)
        assertTrue(undone.session.history.canRedo)
        val moved = undone.enter(3, 6)
        assertFalse(moved.session.history.canRedo)
        assertSame(moved, moved.act(Redo))
    }

    @Test
    fun undoAndRedoWithNothingToDoChangeNothing() {
        assertSame(start, start.act(Undo))
        assertSame(start, start.act(Redo))
    }

    @Test
    fun undoAndRedoSelectTheChangedCell() {
        val elsewhere = start.enter(2, 4).act(SelectCell(50))
        assertEquals(2, elsewhere.act(Undo).input.selected)
        assertEquals(2, elsewhere.act(Undo, SelectCell(50), Redo).input.selected)

        val filled = start.act(SelectCell(50), FillAllNotes)
        assertEquals(50, filled.act(Undo).input.selected, "board-wide moves keep the selection")
    }

    @Test
    fun autoNotesIsOneUndoableMove() {
        val filled = start.act(FillAllNotes)
        assertEquals(Candidates.of(1, 2, 4), filled.notes(2))
        for (cell in 0 until 81) {
            if (filled.value(cell) != 0) assertEquals(Candidates.NONE, filled.notes(cell))
            else assertEquals(NotesOps.candidates(filled.session.spec, filled.session.values, cell), filled.notes(cell))
        }
        assertEquals(1, filled.session.history.undo.size)
        assertSame(filled, filled.act(FillAllNotes), "nothing left to fill")
        assertTrue(filled.act(Undo).session.notes.all { it == 0 })
    }

    @Test
    fun historyKeepsTheLatestMoves() {
        val moves = (0 until 250).map { Move(it, listOf(CellChange(it % 81, 0, 1, 0, 0))) }
        val history = moves.fold(History()) { h, m -> h.push(m) }
        assertEquals(History.MAX_MOVES, history.undo.size)
        assertEquals(moves.takeLast(History.MAX_MOVES), history.undo)
    }

    @Test
    fun historySurvivesAJsonRoundTrip() {
        val history = start.note(3, 4).enter(2, 4).act(FillAllNotes, Undo).session.history
        val json = Json.encodeToString(History.serializer(), history)
        assertEquals(history, Json.decodeFromString(History.serializer(), json))
    }
}
