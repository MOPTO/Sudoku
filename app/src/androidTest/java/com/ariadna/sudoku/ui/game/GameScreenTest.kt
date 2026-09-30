package com.ariadna.sudoku.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.MistakeMode
import com.ariadna.sudoku.core.game.reduce
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.data.settings.UserSettings
import com.ariadna.sudoku.ui.game.components.CellHighlightKey
import com.ariadna.sudoku.ui.game.components.CellValueStateKey
import com.ariadna.sudoku.ui.theme.SudokuTheme
import org.junit.Rule
import org.junit.Test

/** The stateless [GameScreen] driven by the real reducer and mapper, without a ViewModel. */
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(settings: UserSettings = UserSettings()) {
        compose.setContent {
            var state by remember { mutableStateOf(classic()) }
            SudokuTheme {
                GameScreen(
                    state = GameUiMapper.map(state, settings, ScreenState()),
                    timerText = { "" },
                    onAction = { action ->
                        if (action is GameScreenAction.Game) state = state.reduce(action.action, settings.rules)
                    },
                    onBack = {},
                    onOpenGame = {},
                )
            }
        }
    }

    private fun cell(index: Int) = compose.onNodeWithTag("cell_$index")

    private fun highlight(value: CellHighlight) = SemanticsMatcher.expectValue(CellHighlightKey, value)

    private fun valueState(value: ValueState) = SemanticsMatcher.expectValue(CellValueStateKey, value)

    @Test
    fun tappingACellHighlightsItAndItsPeers() {
        setContent()
        cell(2).performClick()
        cell(2).assert(highlight(CellHighlight.Selected))
        cell(8).assert(highlight(CellHighlight.Peer))    // row
        cell(38).assert(highlight(CellHighlight.Peer))   // column
        cell(10).assert(highlight(CellHighlight.Peer))   // box
        cell(40).assert(highlight(CellHighlight.None))
    }

    @Test
    fun aWrongDigitShowsTheErrorState() {
        setContent()
        cell(2).performClick()
        compose.onNodeWithTag("pad_9").performClick()   // solution is 4; 9 also repeats the given in cell 19
        cell(2).assert(valueState(ValueState.Wrong))
        cell(19).assert(valueState(ValueState.Conflict))
        compose.onNodeWithTag("pad_4").performClick()   // replace with the right digit
        cell(2).assert(valueState(ValueState.Normal))
    }

    @Test
    fun checkIsOfferedOnlyWhenMistakesAreOff() {
        setContent(UserSettings(mistakeMode = MistakeMode.Immediate))
        compose.onNodeWithContentDescription("Undo").assertExists()   // the toolbar is there, so the next check means something
        compose.onNodeWithContentDescription("Check").assertDoesNotExist()
    }

    @Test
    fun checkMarksWrongCellsInOffMode() {
        setContent(UserSettings(mistakeMode = MistakeMode.Off))
        cell(2).performClick()
        compose.onNodeWithTag("pad_9").performClick()
        cell(2).assert(valueState(ValueState.Normal))
        compose.onNodeWithContentDescription("Check").assertIsDisplayed().performClick()
        cell(2).assert(valueState(ValueState.Wrong))
    }

    private fun classic(): GameState {
        val spec = GridSpec.Classic
        val givens = Grid.parse(spec, "530070000600195000098000060800060003400803001700020006060000280000419005000080079")
        return GameState(
            GameSession(
                id = 1,
                spec = spec,
                difficulty = Difficulty.Easy,
                seed = 0,
                timed = false,
                givens = givens,
                solution = Grid.decodeValues(spec, "534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
                values = givens.values,
                notes = IntArray(spec.cellCount),
            ),
        )
    }
}
