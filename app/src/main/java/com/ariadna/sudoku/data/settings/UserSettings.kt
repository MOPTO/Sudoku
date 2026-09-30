package com.ariadna.sudoku.data.settings

import com.ariadna.sudoku.core.game.GameRules
import com.ariadna.sudoku.core.game.MistakeMode

/** How the number pad works: pick a cell then a digit, or lock a digit then tap cells (§7.2). */
enum class InputMode { CellFirst, DigitFirst }

/**
 * Player settings (§6.2). Holds the game-related keys for now; theme, haptics and the remembered
 * New-game choice arrive with DataStore in milestones 7 and 8.
 */
data class UserSettings(
    val mistakeMode: MistakeMode = MistakeMode.Immediate,
    val detectWrongNotes: Boolean = false,
    val highlightRowCol: Boolean = true,
    val highlightBox: Boolean = true,
    val highlightSameDigit: Boolean = true,
    val highlightSameDigitNotes: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    /** Dims a pad digit once all N of it are placed. */
    val hideCompletedDigits: Boolean = true,
    val showRemainingCount: Boolean = true,
    val inputMode: InputMode = InputMode.CellFirst,
) {
    /** The part of the settings the reducer depends on. */
    val rules: GameRules get() = GameRules(mistakeMode = mistakeMode, autoRemoveNotes = autoRemoveNotes)
}
