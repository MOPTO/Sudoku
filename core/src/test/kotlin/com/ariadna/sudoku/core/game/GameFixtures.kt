package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.TestGrids
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec

/** Shared game states for the reducer tests. */
object GameFixtures {

    // Cells of the classic puzzle used throughout: 0 is a given 5; 2 (solution 4) and 3 (solution 6) are
    // empty in row 0; 10 (solution 7) is empty in box 0; 80 is a given 9.

    fun classic(): GameState = state(GridSpec.Classic, TestGrids.CLASSIC_PUZZLE, TestGrids.CLASSIC_SOLUTION)

    /** 4×4 board that is solved except for [blanks] (solution digit 1 in cells 0 and 15). */
    fun mini(vararg blanks: Int): GameState {
        val puzzle = TestGrids.MINI_SOLUTION.toCharArray().also { chars -> blanks.forEach { chars[it] = '0' } }
        return state(GridSpec.Mini4, String(puzzle), TestGrids.MINI_SOLUTION)
    }

    fun state(spec: GridSpec, puzzle: String, solution: String): GameState {
        val givens = Grid.parse(spec, puzzle)
        return GameState(
            GameSession(
                id = 1,
                spec = spec,
                difficulty = Difficulty.Easy,
                seed = 0,
                timed = false,
                givens = givens,
                solution = Grid.decodeValues(spec, solution),
                values = givens.values,
                notes = IntArray(spec.cellCount),
            ),
        )
    }

    fun GameState.act(vararg actions: GameAction, rules: GameRules = GameRules()): GameState =
        actions.fold(this) { state, action -> state.reduce(action, rules) }

    /** Selects [cell] and enters [digit] as a value. */
    fun GameState.enter(cell: Int, digit: Int, rules: GameRules = GameRules()): GameState =
        act(GameAction.SelectCell(cell), GameAction.Input(digit), rules = rules)

    /** Adds [digits] to the notes of [cell] (leaves notes mode off). */
    fun GameState.note(cell: Int, vararg digits: Int): GameState {
        val actions = listOf(GameAction.ToggleNotesMode, GameAction.SelectCell(cell)) +
            digits.map { GameAction.Input(it) } + GameAction.ToggleNotesMode
        return act(*actions.toTypedArray())
    }

    fun GameState.value(cell: Int): Int = session.values[cell]

    fun GameState.notes(cell: Int): Candidates = Candidates(session.notes[cell])
}
