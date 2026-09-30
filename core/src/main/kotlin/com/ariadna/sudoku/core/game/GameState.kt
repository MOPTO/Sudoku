package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.hint.Hint

/** A shown hint and how much of it is revealed: 1 = nudge, 2 = technique, 3 = reveal (§4.8). */
data class HintState(val hint: Hint, val level: Int = 1) {
    init {
        require(level in 1..MAX_LEVEL) { "hint level must be in 1..$MAX_LEVEL, was $level" }
    }

    companion object {
        const val MAX_LEVEL = 3
    }
}

/** Transient input state. Never persisted; defaults are restored on load. */
data class InputState(
    val selected: Int? = null,
    val notesMode: Boolean = false,
    /** Digit-first input: tapping a cell enters this digit. */
    val lockedDigit: Int? = null,
    val hint: HintState? = null,
    /** Result of a manual Check, cleared by the VM after 3 s. */
    val checkedWrongCells: Set<Int> = emptySet(),
)

data class GameState(val session: GameSession, val input: InputState = InputState())
