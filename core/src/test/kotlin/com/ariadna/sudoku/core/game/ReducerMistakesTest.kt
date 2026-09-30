package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.CheckBoard
import com.ariadna.sudoku.core.game.GameAction.ClearCheck
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.Redo
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ToggleNotesMode
import com.ariadna.sudoku.core.game.GameAction.Undo
import com.ariadna.sudoku.core.game.GameFixtures.act
import com.ariadna.sudoku.core.game.GameFixtures.enter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ReducerMistakesTest {

    private val start = GameFixtures.classic()
    private val immediate = GameRules(MistakeMode.Immediate)
    private val off = GameRules(MistakeMode.Off)

    @Test
    fun immediateModeCountsWrongDigits() {
        val state = start.enter(2, 1, immediate)          // wrong (solution 4)
        assertEquals(1, state.session.mistakes)
        assertEquals(1, state.enter(2, 4, immediate).session.mistakes, "a right digit is not counted")
        assertEquals(1, state.act(Input(1), rules = immediate).session.mistakes, "erasing by re-entering is not counted")
        assertEquals(2, state.enter(3, 1, immediate).session.mistakes)
    }

    @Test
    fun undoDoesNotSubtractAndRedoDoesNotAdd() {
        val state = start.enter(2, 1, immediate).enter(3, 1, immediate)
        assertEquals(2, state.act(Undo, Undo, rules = immediate).session.mistakes)
        assertEquals(2, state.act(Undo, Undo, Redo, Redo, rules = immediate).session.mistakes)
    }

    @Test
    fun otherModesAndNotesNeverCount() {
        for (mode in listOf(MistakeMode.Off, MistakeMode.Conflicts)) {
            assertEquals(0, start.enter(2, 1, GameRules(mode)).session.mistakes, "$mode")
        }
        assertEquals(0, start.act(ToggleNotesMode, SelectCell(2), Input(1), rules = immediate).session.mistakes)
    }

    @Test
    fun checkMarksWrongCellsAndCountsAsAHint() {
        val state = start.enter(2, 1, off).enter(3, 6, off)   // 2 wrong, 3 right
        val checked = state.act(CheckBoard, rules = off)
        assertEquals(setOf(2), checked.input.checkedWrongCells)
        assertEquals(1, checked.session.hintsUsed)

        val cleared = checked.act(ClearCheck, rules = off)
        assertEquals(emptySet(), cleared.input.checkedWrongCells)
        assertSame(checked.session, cleared.session)
        assertSame(cleared, cleared.act(ClearCheck, rules = off))
    }

    @Test
    fun checkIsOnlyOfferedWhenMistakesAreOff() {
        val state = start.enter(2, 1)
        assertSame(state, state.act(CheckBoard, rules = immediate))
        assertSame(state, state.act(CheckBoard, rules = GameRules(MistakeMode.Conflicts)))
    }

    @Test
    fun changingACheckedCellDropsItsMark() {
        val checked = start.enter(2, 1, off).enter(3, 1, off).act(CheckBoard, rules = off)
        assertEquals(setOf(2, 3), checked.input.checkedWrongCells)
        assertEquals(setOf(3), checked.enter(2, 4, off).input.checkedWrongCells)
        assertEquals(setOf(2), checked.act(Undo, rules = off).input.checkedWrongCells)
    }
}
