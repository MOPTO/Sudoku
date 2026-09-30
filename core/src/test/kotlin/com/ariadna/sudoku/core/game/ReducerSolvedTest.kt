package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.AdvanceHint
import com.ariadna.sudoku.core.game.GameAction.CheckBoard
import com.ariadna.sudoku.core.game.GameAction.Erase
import com.ariadna.sudoku.core.game.GameAction.FillAllNotes
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.Redo
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ShowHint
import com.ariadna.sudoku.core.game.GameAction.Undo
import com.ariadna.sudoku.core.game.GameFixtures.act
import com.ariadna.sudoku.core.game.GameFixtures.enter
import com.ariadna.sudoku.core.hint.Hint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ReducerSolvedTest {

    private val start = GameFixtures.mini(0, 15)

    @Test
    fun theLastRightDigitSolvesTheGame() {
        val half = start.enter(0, 1)
        assertEquals(GameStatus.InProgress, half.session.status)
        assertEquals(GameStatus.InProgress, half.enter(15, 2).session.status, "a full but wrong board is not solved")
        assertEquals(GameStatus.Solved, half.enter(15, 1).session.status)
        assertEquals(GameStatus.Solved, half.enter(15, 2).enter(15, 1).session.status)
    }

    @Test
    fun aHintRevealCanSolveTheGameAndClearsTheHintAndCheckMarks() {
        val off = GameRules(MistakeMode.Off)
        val checked = start.enter(0, 3, off).act(CheckBoard, rules = off)
        assertEquals(setOf(0), checked.input.checkedWrongCells)
        val solved = checked.enter(15, 1, off).act(ShowHint(Hint.FixMistake(0)), AdvanceHint, AdvanceHint, rules = off)
        assertEquals(GameStatus.Solved, solved.session.status)
        assertNull(solved.input.hint)
        assertEquals(emptySet(), solved.input.checkedWrongCells)
    }

    @Test
    fun aSolvedGameIgnoresEveryMove() {
        val solved = start.enter(0, 1).enter(15, 1)
        for (action in listOf(Input(2), Erase, Undo, Redo, FillAllNotes, ShowHint(Hint.Reveal(0)), CheckBoard)) {
            assertSame(solved.session, solved.act(action, rules = GameRules(MistakeMode.Off)).session, "$action")
        }
        assertEquals(5, solved.act(SelectCell(5)).input.selected, "selection still works")
    }
}
