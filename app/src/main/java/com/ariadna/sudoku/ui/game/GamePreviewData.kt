package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.core.game.GameRules
import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.reduce
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.data.settings.UserSettings

/** Fixed game states for Compose previews. */
internal object GamePreviewData {

    private const val CLASSIC_PUZZLE = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private const val CLASSIC_SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    private const val MINI_PUZZLE = "1200001221000021"
    private const val MINI_SOLUTION = "1234341221434321"

    /** 9×9 with a few moves: a correct digit, a wrong one (error state), notes, and cell 20 selected. */
    fun classicInProgress(timed: Boolean = false, paused: Boolean = false): GameUiState.Ready {
        val state = start(GridSpec.Classic, CLASSIC_PUZZLE, CLASSIC_SOLUTION, timed).act(
            GameAction.SelectCell(2), GameAction.Input(4),
            GameAction.SelectCell(3), GameAction.Input(9),
            GameAction.ToggleNotesMode, GameAction.SelectCell(10), GameAction.Input(2), GameAction.Input(7),
            GameAction.ToggleNotesMode, GameAction.SelectCell(20),
        )
        return GameUiMapper.map(state, UserSettings(), ScreenState(paused = paused))
    }

    fun miniInProgress(): GameUiState.Ready {
        val state = start(GridSpec.Mini4, MINI_PUZZLE, MINI_SOLUTION, timed = false)
            .act(GameAction.SelectCell(5), GameAction.Input(4))
        return GameUiMapper.map(state, UserSettings(), ScreenState())
    }

    private fun start(spec: GridSpec, puzzle: String, solution: String, timed: Boolean): GameState {
        val givens = Grid.parse(spec, puzzle)
        return GameState(
            GameSession(
                id = 1,
                spec = spec,
                difficulty = Difficulty.Medium,
                seed = 0,
                timed = timed,
                givens = givens,
                solution = Grid.decodeValues(spec, solution),
                values = givens.values,
                notes = IntArray(spec.cellCount),
            ),
        )
    }

    private fun GameState.act(vararg actions: GameAction): GameState =
        actions.fold(this) { state, action -> state.reduce(action, GameRules()) }
}
