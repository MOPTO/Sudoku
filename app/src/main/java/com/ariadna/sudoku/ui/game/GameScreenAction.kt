package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameAction

/** UI-level actions of the game screen. Pure navigation (back, open a game) stays as plain lambdas. */
sealed interface GameScreenAction {
    /** Forwarded to the pure reducer. */
    data class Game(val action: GameAction) : GameScreenAction

    /** Timed games only: the player pauses (the overlay hides the board) or resumes. */
    data object Pause : GameScreenAction
    data object Resume : GameScreenAction

    /** Lifecycle: the timer only runs while the screen is RESUMED, and leaving it saves at once. */
    data object ScreenResumed : GameScreenAction
    data object ScreenPaused : GameScreenAction

    /** From the completion dialog: a new game with the same size, difficulty and timer choice. */
    data object NextPuzzle : GameScreenAction

    /** Closes the completion dialog and leaves the solved board visible. */
    data object DismissCompletion : GameScreenAction
}
