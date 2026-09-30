package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.MistakeChecker
import com.ariadna.sudoku.core.game.MistakeMode
import com.ariadna.sudoku.core.model.Digit
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.box
import com.ariadna.sudoku.core.model.col
import com.ariadna.sudoku.core.model.row
import com.ariadna.sudoku.data.settings.InputMode
import com.ariadna.sudoku.data.settings.UserSettings

/** Screen-level state the ViewModel keeps next to the [GameState]. */
data class ScreenState(
    /** The player pressed Pause (timed games only). */
    val paused: Boolean = false,
    /** Time of the solved game, set by the ViewModel once it has stopped the timer (0 when untimed). */
    val solvedElapsedMs: Long? = null,
    val completionDismissed: Boolean = false,
    val nextGameId: Long? = null,
)

/** GameState + settings → [GameUiState.Ready]. All derived work (highlights, errors, counts) happens here. */
object GameUiMapper {

    fun map(state: GameState, settings: UserSettings, screen: ScreenState): GameUiState.Ready {
        val session = state.session
        val input = state.input
        val spec = session.spec
        val paused = screen.paused && session.timed && !session.isSolved
        val interactive = !session.isSolved && !paused
        val errors = errorCells(state, settings.mistakeMode)
        val wrongNotes = if (settings.detectWrongNotes) MistakeChecker.wrongNoteCells(session) else emptySet()

        val selected = input.selected
        val focusDigit = when (settings.inputMode) {
            InputMode.DigitFirst -> input.lockedDigit ?: 0
            InputMode.CellFirst -> selected?.let { session.values[it] } ?: 0
        }
        val cells = List(spec.cellCount) { cell ->
            val value = session.values[cell]
            val valueState = errors[cell] ?: ValueState.Normal
            val highlight = when {
                cell == selected -> CellHighlight.Selected
                valueState != ValueState.Normal -> CellHighlight.Conflict
                settings.highlightSameDigit && focusDigit != 0 && value == focusDigit -> CellHighlight.SameDigit
                selected != null && isPeer(spec, selected, cell, settings) -> CellHighlight.Peer
                else -> CellHighlight.None
            }
            CellUi(
                index = cell,
                value = value,
                isGiven = session.isGiven(cell),
                notes = session.notes[cell],
                highlight = highlight,
                valueState = valueState,
                notesHighlightedDigit = if (settings.highlightSameDigitNotes) focusDigit else 0,
                notesWrong = cell in wrongNotes,
            )
        }

        val placed = IntArray(spec.size + 1)
        for (v in session.values) placed[v]++
        val digits = (1..spec.size).map { d ->
            PadDigitUi(
                digit = d,
                label = Digit.toChar(d),
                remaining = (spec.size - placed[d]).coerceAtLeast(0),
                dimmed = settings.hideCompletedDigits && placed[d] >= spec.size,
                locked = settings.inputMode == InputMode.DigitFirst && input.lockedDigit == d,
            )
        }

        val countsMistakes = settings.mistakeMode == MistakeMode.Immediate
        val completion = screen.solvedElapsedMs
            ?.takeIf { session.isSolved && !screen.completionDismissed }
            ?.let { elapsed ->
                CompletionUi(
                    elapsedMs = if (session.timed) elapsed else null,
                    mistakes = if (countsMistakes) session.mistakes else null,
                    hintsUsed = session.hintsUsed,
                )
            }

        return GameUiState.Ready(
            board = BoardUi(spec.size, spec.boxRows, spec.boxCols, cells),
            pad = PadUi(
                digits = digits,
                showRemaining = settings.showRemainingCount,
                notesMode = input.notesMode,
                digitFirst = settings.inputMode == InputMode.DigitFirst,
                enabled = interactive,
            ),
            toolbar = ToolbarUi(
                canUndo = session.history.canUndo,
                canRedo = session.history.canRedo,
                notesMode = input.notesMode,
                showCheck = settings.mistakeMode == MistakeMode.Off,
                enabled = interactive,
            ),
            topBar = TopBarUi(spec.size, session.difficulty, if (countsMistakes) session.mistakes else null, session.timed),
            paused = paused,
            completion = completion,
            nextGameId = screen.nextGameId,
        )
    }

    /** Cells drawn as errors for the current mistake mode (§7.3). Wrong wins over Conflict. */
    private fun errorCells(state: GameState, mode: MistakeMode): Map<Int, ValueState> {
        val session = state.session
        val result = HashMap<Int, ValueState>()
        when (mode) {
            MistakeMode.Off -> state.input.checkedWrongCells.forEach { result[it] = ValueState.Wrong }
            MistakeMode.Conflicts -> MistakeChecker.conflicts(session.spec, session.values).forEach { result[it] = ValueState.Conflict }
            MistakeMode.Immediate -> {
                MistakeChecker.conflicts(session.spec, session.values).forEach { result[it] = ValueState.Conflict }
                MistakeChecker.wrongCells(session).forEach { result[it] = ValueState.Wrong }
            }
        }
        return result
    }

    private fun isPeer(spec: GridSpec, selected: Int, cell: Int, settings: UserSettings): Boolean =
        (settings.highlightRowCol && (spec.row(cell) == spec.row(selected) || spec.col(cell) == spec.col(selected))) ||
            (settings.highlightBox && spec.box(cell) == spec.box(selected))
}
