package com.ariadna.sudoku.ui.game

import androidx.compose.runtime.Immutable
import com.ariadna.sudoku.core.grading.Difficulty

/** Everything the game screen draws. Built by [GameUiMapper]; composables only draw it. */
sealed interface GameUiState {
    data object Loading : GameUiState

    /** The game id does not exist (e.g. after process death while games are only kept in memory). */
    data object NotFound : GameUiState

    @Immutable
    data class Ready(
        val board: BoardUi,
        val pad: PadUi,
        val toolbar: ToolbarUi,
        val topBar: TopBarUi,
        /** Timed games only: the pause overlay hides the board. */
        val paused: Boolean,
        /** Non-null while the completion dialog is shown. */
        val completion: CompletionUi?,
        /** Set once "Next puzzle" has created a game; the screen then navigates to it. */
        val nextGameId: Long?,
    ) : GameUiState
}

/** Background layer of a cell, highest priority first (§7.2). */
enum class CellHighlight { Selected, Conflict, SameDigit, Peer, None }

/** How a cell's digit is drawn (§7.3). */
enum class ValueState { Normal, Wrong, Conflict }

@Immutable
data class BoardUi(
    val size: Int,
    val boxRows: Int,
    val boxCols: Int,
    val cells: List<CellUi>,
)

@Immutable
data class CellUi(
    val index: Int,
    /** 0 = empty. */
    val value: Int,
    val isGiven: Boolean,
    /** Candidates bitmask (pencil marks). */
    val notes: Int,
    val highlight: CellHighlight,
    val valueState: ValueState,
    /** Digit to emphasise inside the notes (0 = none). */
    val notesHighlightedDigit: Int,
    /** The notes miss the cell's true digit (wrong-note detection). */
    val notesWrong: Boolean,
)

@Immutable
data class PadUi(
    val digits: List<PadDigitUi>,
    val showRemaining: Boolean,
    val notesMode: Boolean,
    /** Digit-first input: a tap locks the digit instead of entering it. */
    val digitFirst: Boolean,
    val enabled: Boolean,
)

@Immutable
data class PadDigitUi(
    val digit: Int,
    val label: Char,
    val remaining: Int,
    /** All N placed and the player wants completed digits dimmed. */
    val dimmed: Boolean,
    /** Digit-first mode: this digit is locked. */
    val locked: Boolean,
)

@Immutable
data class ToolbarUi(
    val canUndo: Boolean,
    val canRedo: Boolean,
    val notesMode: Boolean,
    /** Manual Check exists only when mistakes are not shown otherwise. */
    val showCheck: Boolean,
    val enabled: Boolean,
)

@Immutable
data class TopBarUi(
    val size: Int,
    val difficulty: Difficulty,
    /** Shown only in Immediate mode. */
    val mistakes: Int?,
    val timed: Boolean,
)

@Immutable
data class CompletionUi(
    /** Null for untimed games. */
    val elapsedMs: Long?,
    /** Null unless mistakes are counted (Immediate mode). */
    val mistakes: Int?,
    val hintsUsed: Int,
)
