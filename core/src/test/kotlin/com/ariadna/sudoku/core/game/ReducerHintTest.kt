package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.AdvanceHint
import com.ariadna.sudoku.core.game.GameAction.DismissHint
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ShowHint
import com.ariadna.sudoku.core.game.GameAction.Undo
import com.ariadna.sudoku.core.game.GameFixtures.act
import com.ariadna.sudoku.core.game.GameFixtures.enter
import com.ariadna.sudoku.core.game.GameFixtures.note
import com.ariadna.sudoku.core.game.GameFixtures.notes
import com.ariadna.sudoku.core.game.GameFixtures.value
import com.ariadna.sudoku.core.hint.Hint
import com.ariadna.sudoku.core.logic.Elimination
import com.ariadna.sudoku.core.logic.Placement
import com.ariadna.sudoku.core.logic.Step
import com.ariadna.sudoku.core.logic.Technique
import com.ariadna.sudoku.core.model.Candidates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ReducerHintTest {

    private val start = GameFixtures.classic()

    /** Shows [hint] and advances it to the reveal level. */
    private fun GameState.reveal(hint: Hint): GameState = act(ShowHint(hint), AdvanceHint, AdvanceHint)

    @Test
    fun eachLevelCountsAsOneHintAndTheLastRevealsIt() {
        val shown = start.act(ShowHint(Hint.Reveal(2)))
        assertEquals(HintState(Hint.Reveal(2), 1), shown.input.hint)
        assertEquals(1, shown.session.hintsUsed)
        assertEquals(0, shown.value(2))

        val technique = shown.act(AdvanceHint)
        assertEquals(2, technique.input.hint?.level)
        assertEquals(2, technique.session.hintsUsed)
        assertEquals(0, technique.value(2))

        val revealed = technique.act(AdvanceHint)
        assertEquals(3, revealed.input.hint?.level, "the hint stays shown after the reveal")
        assertEquals(3, revealed.session.hintsUsed)
        assertEquals(4, revealed.value(2))
        assertSame(revealed, revealed.act(AdvanceHint))
        assertSame(start, start.act(AdvanceHint), "no hint shown")
    }

    @Test
    fun dismissOnlyTouchesTheInputState() {
        val shown = start.act(ShowHint(Hint.Reveal(2)))
        val dismissed = shown.act(DismissHint)
        assertNull(dismissed.input.hint)
        assertSame(shown.session, dismissed.session)
        assertSame(start, start.act(DismissHint))
    }

    @Test
    fun movesDismissTheHintButSelectionDoesNot() {
        val shown = start.act(ShowHint(Hint.Reveal(2)))
        assertEquals(shown.input.hint, shown.act(SelectCell(40)).input.hint)
        assertNull(shown.enter(3, 6).input.hint)
        assertNull(start.enter(3, 6).act(ShowHint(Hint.Reveal(2)), Undo).input.hint)
    }

    @Test
    fun revealsPlaceTheSolutionDigitAsAnUndoableMove() {
        val mistakes = start.enter(2, 1).session.mistakes
        val fixed = start.enter(2, 1).reveal(Hint.FixMistake(2))
        assertEquals(4, fixed.value(2))
        assertEquals(mistakes, fixed.session.mistakes)
        assertEquals(1, fixed.act(Undo).value(2))

        val revealed = start.note(3, 4, 6).reveal(Hint.Reveal(2))
        assertEquals(4, revealed.value(2))
        assertEquals(Candidates.of(6), revealed.notes(3), "auto-remove applies to reveals too")
    }

    @Test
    fun badNoteRevealAddsTheMissingDigit() {
        assertEquals(Candidates.of(1, 4), start.note(2, 1).reveal(Hint.BadNote(2, 4)).notes(2))
    }

    @Test
    fun stepRevealAppliesThePlacementOrTheEliminations() {
        val single = Step(Technique.NakedSingle, null, listOf(2), Candidates.of(4), placement = Placement(2, 4))
        assertEquals(4, start.reveal(Hint.Step(single)).value(2))

        val eliminations = listOf(Elimination(2, 1), Elimination(2, 2), Elimination(3, 1), Elimination(4, 1))
        val step = Step(Technique.LockedPointing, 0, listOf(2, 3), Candidates.of(1), eliminations = eliminations)
        val noted = start.note(2, 1, 2, 4).note(3, 1, 6)
        val applied = noted.reveal(Hint.Step(step))
        assertEquals(Candidates.of(4), applied.notes(2))
        assertEquals(Candidates.of(6), applied.notes(3))
        assertEquals(noted.session.history.undo.size + 1, applied.session.history.undo.size)
        assertEquals(Candidates.of(1, 2, 4), applied.act(Undo).notes(2))
    }

    @Test
    fun aStaleRevealLeavesTheBoardAlone() {
        val placed = start.enter(2, 4)
        for (hint in listOf(Hint.Reveal(2), Hint.FixMistake(2), Hint.FixMistake(0), Hint.BadNote(2, 4))) {
            val revealed = placed.reveal(hint)
            assertEquals(placed.session.history, revealed.session.history, "$hint")
            assertEquals(3, revealed.session.hintsUsed, "$hint still counts")
        }
    }
}
