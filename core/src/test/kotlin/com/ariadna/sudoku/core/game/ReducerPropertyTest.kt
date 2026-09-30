package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.generator.PuzzleGenerator
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.hint.Hint
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Random action sequences over generated puzzles: the reducer's invariants must hold after every action. */
class ReducerPropertyTest {

    private val steps = 150   // below History.MAX_MOVES, so undoing everything reaches the start

    @Test
    fun invariantsHoldForRandomPlay() = runBlocking {
        for (spec in listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)) {
            for (difficulty in DifficultyProfile.supported(spec)) {
                for (seed in 0L until 10L) {
                    val puzzle = PuzzleGenerator.generate(spec, difficulty, seed).puzzle
                    play(GameState(GameSession.start(seed, puzzle, timed = false)), Random(seed), "${spec.size}x${spec.size} $difficulty seed=$seed")
                }
            }
        }
    }

    private fun play(start: GameState, random: Random, label: String) {
        val size = start.session.spec.size
        val cells = start.session.spec.cellCount
        val givens = start.session.givens
        var state = start
        repeat(steps) { step ->
            val rules = GameRules(MistakeMode.entries.random(random), random.nextBoolean())
            val action = randomAction(state, random, size, cells)
            val values = state.session.values.copyOf()
            val notes = state.session.notes.copyOf()
            val next = state.reduce(action, rules)
            val at = "$label step=$step $action"

            assertTrue(values.contentEquals(state.session.values) && notes.contentEquals(state.session.notes), "$at mutated the old state")
            if (action.isInputOnly()) assertSame(state.session, next.session, "$at changed the session")
            for (cell in 0 until cells) {
                if (givens.isGiven(cell)) assertEquals(givens[cell], next.session.values[cell], "$at changed a given")
                if (next.session.values[cell] != 0) assertEquals(0, next.session.notes[cell], "$at left notes on a filled cell")
            }
            assertTrue(next.session.mistakes >= state.session.mistakes, "$at decreased mistakes")
            assertTrue(next.session.hintsUsed >= state.session.hintsUsed, "$at decreased hints")
            assertEquals(next.session.values.contentEquals(next.session.solution), next.session.isSolved, "$at solved flag")
            state = next
        }
        if (state.session.isSolved) return
        while (state.session.history.canUndo) state = state.reduce(GameAction.Undo, GameRules())
        assertTrue(state.session.values.contentEquals(start.session.values), "$label: undo all restores the values")
        assertTrue(state.session.notes.all { it == 0 }, "$label: undo all restores the notes")
    }

    private fun GameAction.isInputOnly(): Boolean =
        this is GameAction.MoveSelection || this == GameAction.ToggleNotesMode || this is GameAction.LockDigit ||
            this == GameAction.DismissHint || this == GameAction.ClearCheck

    private fun randomAction(state: GameState, random: Random, size: Int, cells: Int): GameAction =
        when (random.nextInt(14)) {
            0, 1 -> GameAction.SelectCell(random.nextInt(cells))
            2, 3, 4 -> GameAction.Input(random.nextInt(1, size + 1))
            5 -> GameAction.Erase
            6 -> GameAction.ToggleNotesMode
            7 -> GameAction.Undo
            8 -> GameAction.Redo
            9 -> GameAction.MoveSelection(random.nextInt(-1, 2), random.nextInt(-1, 2))
            10 -> GameAction.LockDigit(if (random.nextInt(3) == 0) random.nextInt(1, size + 1) else null)
            11 -> if (random.nextInt(10) == 0) GameAction.FillAllNotes else GameAction.CheckBoard
            12 -> {
                val empty = (0 until cells).filter { state.session.values[it] == 0 }
                if (empty.isEmpty()) GameAction.ClearCheck else GameAction.ShowHint(Hint.Reveal(empty.random(random)))
            }
            else -> if (random.nextBoolean()) GameAction.AdvanceHint else GameAction.DismissHint
        }
}
