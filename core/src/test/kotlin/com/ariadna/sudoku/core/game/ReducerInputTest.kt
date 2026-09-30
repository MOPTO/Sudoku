package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.Erase
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.LockDigit
import com.ariadna.sudoku.core.game.GameAction.MoveSelection
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ToggleNotesMode
import com.ariadna.sudoku.core.game.GameFixtures.act
import com.ariadna.sudoku.core.game.GameFixtures.enter
import com.ariadna.sudoku.core.game.GameFixtures.note
import com.ariadna.sudoku.core.game.GameFixtures.notes
import com.ariadna.sudoku.core.game.GameFixtures.value
import com.ariadna.sudoku.core.model.Candidates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReducerInputTest {

    private val start = GameFixtures.classic()

    @Test
    fun inputOnlyActionsKeepTheSameSession() {
        for (action in listOf(SelectCell(2), ToggleNotesMode, LockDigit(4), MoveSelection(1, 0))) {
            assertSame(start.session, start.act(action).session, "$action")
        }
        val selected = start.act(SelectCell(2))
        assertEquals(2, selected.input.selected)
        assertSame(selected, selected.act(SelectCell(2)), "reselecting changes nothing")
        assertSame(start, start.act(SelectCell(-1)))
        assertSame(start, start.act(SelectCell(81)))
    }

    @Test
    fun moveSelectionStartsAtTheFirstCellAndIsClamped() {
        val first = start.act(MoveSelection(0, 1))
        assertEquals(0, first.input.selected)
        assertSame(first, first.act(MoveSelection(-1, -1)))
        assertEquals(11, first.act(MoveSelection(1, 2)).input.selected)
        assertEquals(80, first.act(MoveSelection(20, 20)).input.selected)
    }

    @Test
    fun inputPlacesTheDigitAndClearsTheCellsNotes() {
        val state = start.note(2, 1, 4).enter(2, 4)
        assertEquals(4, state.value(2))
        assertEquals(Candidates.NONE, state.notes(2))
    }

    @Test
    fun inputOverwritesAndTheSameDigitErases() {
        assertEquals(4, start.enter(2, 1).enter(2, 4).value(2))
        val erased = start.enter(2, 4).enter(2, 4)
        assertEquals(0, erased.value(2))
        assertEquals(2, erased.session.history.undo.size)
    }

    @Test
    fun givensAndInvalidInputAreIgnored() {
        val onGiven = start.act(SelectCell(0))
        assertSame(onGiven, onGiven.act(Input(9)))
        assertSame(onGiven, onGiven.act(Erase))
        assertSame(start, start.act(Input(4)), "nothing selected")
        val selected = start.act(SelectCell(2))
        assertSame(selected, selected.act(Input(0)))
        assertSame(selected, selected.act(Input(10)))
    }

    @Test
    fun notesModeTogglesNotesOnEmptyCellsOnly() {
        val state = start.act(ToggleNotesMode, SelectCell(2), Input(1), Input(4), Input(1))
        assertEquals(Candidates.of(4), state.notes(2))
        assertEquals(0, state.value(2))
        val filled = start.enter(2, 4).act(ToggleNotesMode)
        assertSame(filled.session, filled.act(Input(1)).session)
    }

    @Test
    fun eraseClearsTheValueThenTheNotes() {
        assertEquals(0, start.enter(2, 4).act(Erase).value(2))
        assertEquals(Candidates.NONE, start.note(2, 1, 4).act(Erase).notes(2))
        val empty = start.act(SelectCell(2))
        assertSame(empty, empty.act(Erase))
    }

    @Test
    fun autoRemoveNotesClearsTheDigitFromPeersOnly() {
        val noted = start.note(3, 4, 6).note(10, 4).note(40, 4)   // 3: same row, 10: same box, 40: no peer
        val on = noted.enter(2, 4)
        assertEquals(Candidates.of(6), on.notes(3))
        assertEquals(Candidates.NONE, on.notes(10))
        assertEquals(Candidates.of(4), on.notes(40))

        val off = noted.enter(2, 4, GameRules(autoRemoveNotes = false))
        assertEquals(Candidates.of(4, 6), off.notes(3))
        assertEquals(Candidates.of(4), off.notes(10))
    }

    @Test
    fun lockedDigitIsEnteredByTappingCells() {
        val locked = start.act(LockDigit(4), SelectCell(2))
        assertEquals(4, locked.value(2))
        assertEquals(2, locked.input.selected)
        assertEquals(0, locked.act(SelectCell(2)).value(2), "tapping the same digit again erases it")
        assertEquals(Candidates.of(4), start.act(LockDigit(4), ToggleNotesMode, SelectCell(2)).notes(2))

        val lockedOnly = start.act(LockDigit(4))
        val onGiven = lockedOnly.act(SelectCell(0))
        assertSame(lockedOnly.session, onGiven.session, "givens ignore the locked digit")
        assertEquals(0, onGiven.input.selected)

        assertSame(start, start.act(LockDigit(0)))
        assertSame(start, start.act(LockDigit(10)))
        assertNull(lockedOnly.act(LockDigit(null)).input.lockedDigit)
    }

    @Test
    fun earlierStatesAreNeverMutated() {
        val values = start.session.values.copyOf()
        val notes = start.session.notes.copyOf()
        start.note(2, 1, 4).enter(2, 4).act(Erase, GameAction.FillAllNotes, GameAction.Undo)
        assertTrue(values.contentEquals(start.session.values))
        assertTrue(notes.contentEquals(start.session.notes))
    }
}
