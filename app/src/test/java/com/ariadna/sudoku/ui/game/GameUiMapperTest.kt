package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameAction.CheckBoard
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.LockDigit
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ToggleNotesMode
import com.ariadna.sudoku.core.game.GameRules
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.MistakeMode
import com.ariadna.sudoku.data.settings.InputMode
import com.ariadna.sudoku.data.settings.UserSettings
import com.ariadna.sudoku.ui.game.TestGames.act
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiMapperTest {

    private fun map(state: GameState, settings: UserSettings = UserSettings(), screen: ScreenState = ScreenState()) =
        GameUiMapper.map(state, settings, screen)

    private fun GameUiState.Ready.highlight(cell: Int) = board.cells[cell].highlight

    private fun GameUiState.Ready.valueState(cell: Int) = board.cells[cell].valueState

    private fun GameState.enter(cell: Int, digit: Int, mode: MistakeMode) =
        act(SelectCell(cell), Input(digit), rules = GameRules(mistakeMode = mode))

    @Test
    fun `selected cell and its row, column and box peers are highlighted`() {
        val ui = map(TestGames.classic().act(SelectCell(2)))
        assertEquals(CellHighlight.Selected, ui.highlight(2))
        assertEquals(CellHighlight.Peer, ui.highlight(8))    // row
        assertEquals(CellHighlight.Peer, ui.highlight(38))   // column
        assertEquals(CellHighlight.Peer, ui.highlight(10))   // box
        assertEquals(CellHighlight.None, ui.highlight(40))
    }

    @Test
    fun `row-column and box toggles switch their layers off`() {
        val state = TestGames.classic().act(SelectCell(2))
        val noRowCol = map(state, UserSettings(highlightRowCol = false))
        assertEquals(CellHighlight.None, noRowCol.highlight(8))
        assertEquals(CellHighlight.None, noRowCol.highlight(38))
        assertEquals(CellHighlight.Peer, noRowCol.highlight(10))

        val noBox = map(state, UserSettings(highlightBox = false))
        assertEquals(CellHighlight.None, noBox.highlight(10))
        assertEquals(CellHighlight.Peer, noBox.highlight(8))
    }

    @Test
    fun `same digit as the selected value is highlighted unless switched off`() {
        val state = TestGames.classic().act(SelectCell(0))   // a given 5
        assertEquals(CellHighlight.SameDigit, map(state).highlight(14))
        assertEquals(CellHighlight.None, map(state, UserSettings(highlightSameDigit = false)).highlight(14))
    }

    @Test
    fun `same digit outranks peer`() {
        // A duplicate 5 in cell 2 (Off mode, so not marked as an error): cell 0 is both a row peer and a 5.
        val settings = UserSettings(mistakeMode = MistakeMode.Off)
        val ui = map(TestGames.classic().enter(2, 5, MistakeMode.Off), settings)
        assertEquals(CellHighlight.SameDigit, ui.highlight(0))
        assertEquals(CellHighlight.Peer, ui.highlight(1))
    }

    @Test
    fun `immediate mode marks wrong digits and conflicting peers and counts mistakes`() {
        // 9 in cell 2 is wrong (solution 4) and repeats the given 9 in cell 19 (box 0).
        val ui = map(TestGames.classic().enter(2, 9, MistakeMode.Immediate))
        assertEquals(ValueState.Wrong, ui.valueState(2))
        assertEquals(CellHighlight.Selected, ui.highlight(2))
        assertEquals(ValueState.Conflict, ui.valueState(19))
        assertEquals(CellHighlight.Conflict, ui.highlight(19))
        assertEquals(1, ui.topBar.mistakes)
        assertFalse(ui.toolbar.showCheck)
    }

    @Test
    fun `conflicts mode marks duplicates only and hides the mistake counter`() {
        val settings = UserSettings(mistakeMode = MistakeMode.Conflicts)
        val duplicate = map(TestGames.classic().enter(2, 9, MistakeMode.Conflicts), settings)
        assertEquals(ValueState.Conflict, duplicate.valueState(2))
        assertEquals(ValueState.Conflict, duplicate.valueState(19))
        assertNull(duplicate.topBar.mistakes)

        // 1 in cell 2 is wrong but conflicts with nothing: not revealed.
        val wrongOnly = map(TestGames.classic().enter(2, 1, MistakeMode.Conflicts), settings)
        assertEquals(ValueState.Normal, wrongOnly.valueState(2))
    }

    @Test
    fun `off mode shows nothing until a check, then the checked wrong cells`() {
        val settings = UserSettings(mistakeMode = MistakeMode.Off)
        val rules = settings.rules
        val entered = TestGames.classic().enter(2, 9, MistakeMode.Off)
        val before = map(entered, settings)
        assertEquals(ValueState.Normal, before.valueState(2))
        assertEquals(ValueState.Normal, before.valueState(19))
        assertTrue(before.toolbar.showCheck)
        assertNull(before.topBar.mistakes)

        val checked = map(entered.act(CheckBoard, rules = rules), settings)
        assertEquals(ValueState.Wrong, checked.valueState(2))
        assertEquals(ValueState.Normal, checked.valueState(19))
    }

    @Test
    fun `digit-first mode highlights the locked digit and marks it on the pad`() {
        val settings = UserSettings(inputMode = InputMode.DigitFirst)
        val ui = map(TestGames.classic().act(LockDigit(5)), settings)
        assertEquals(CellHighlight.SameDigit, ui.highlight(0))
        assertEquals(CellHighlight.SameDigit, ui.highlight(14))
        assertTrue(ui.pad.digitFirst)
        assertTrue(ui.pad.digits[4].locked)
        assertFalse(ui.pad.digits[3].locked)
        assertEquals(5, ui.board.cells[2].notesHighlightedDigit)
    }

    @Test
    fun `pad counts remaining digits and dims completed ones`() {
        val state = TestGames.mini(0, 15)
        val ui = map(state)
        assertEquals(2, ui.pad.digits[0].remaining)
        assertFalse(ui.pad.digits[0].dimmed)
        assertEquals(0, ui.pad.digits[1].remaining)
        assertTrue(ui.pad.digits[1].dimmed)
        assertEquals('2', ui.pad.digits[1].label)

        assertFalse(map(state, UserSettings(hideCompletedDigits = false)).pad.digits[1].dimmed)
    }

    @Test
    fun `wrong notes are flagged only with detection on`() {
        val state = TestGames.classic().act(ToggleNotesMode, SelectCell(2), Input(1), Input(2))   // solution 4
        assertFalse(map(state).board.cells[2].notesWrong)
        assertTrue(map(state, UserSettings(detectWrongNotes = true)).board.cells[2].notesWrong)
        assertEquals(0b11, map(state).board.cells[2].notes)
    }

    @Test
    fun `completion appears once the solve time is known and disappears when dismissed`() {
        val solved = TestGames.mini(0).act(SelectCell(0), Input(1))
        assertNull(map(solved).completion)

        val ui = map(solved, screen = ScreenState(solvedElapsedMs = 0))
        assertNotNull(ui.completion)
        val completion = ui.completion!!
        assertNull(completion.elapsedMs)   // untimed
        assertEquals(0, completion.mistakes)
        assertEquals(0, completion.hintsUsed)
        assertFalse(ui.pad.enabled)
        assertFalse(ui.toolbar.enabled)

        assertNull(map(solved, screen = ScreenState(solvedElapsedMs = 0, completionDismissed = true)).completion)
    }

    @Test
    fun `completion shows the time for timed games and hides mistakes outside immediate mode`() {
        val solved = TestGames.mini(0, timed = true).act(SelectCell(0), Input(1))
        val completion = map(solved, UserSettings(mistakeMode = MistakeMode.Conflicts), ScreenState(solvedElapsedMs = 61_000)).completion!!
        assertEquals(61_000L, completion.elapsedMs)
        assertNull(completion.mistakes)
    }

    @Test
    fun `pause applies only to timed games and disables input`() {
        val paused = map(TestGames.mini(0, timed = true), screen = ScreenState(paused = true))
        assertTrue(paused.paused)
        assertFalse(paused.pad.enabled)
        assertFalse(paused.toolbar.enabled)

        assertFalse(map(TestGames.mini(0), screen = ScreenState(paused = true)).paused)
    }
}
