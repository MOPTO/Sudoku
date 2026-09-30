package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.hint.Hint

/** Everything the player (or the ViewModel on their behalf) can do to a [GameState]. */
sealed interface GameAction {
    data class SelectCell(val index: Int) : GameAction
    data class MoveSelection(val dRow: Int, val dCol: Int) : GameAction   // keyboard / d-pad
    data class Input(val digit: Int) : GameAction
    data object Erase : GameAction
    data object ToggleNotesMode : GameAction
    data class LockDigit(val digit: Int?) : GameAction
    data object Undo : GameAction
    data object Redo : GameAction
    data object FillAllNotes : GameAction              // auto-notes
    data class ShowHint(val hint: Hint) : GameAction   // computed by the VM off the main thread (§4.8)
    data object AdvanceHint : GameAction
    data object DismissHint : GameAction
    data object CheckBoard : GameAction                // one-shot check when MistakeMode = Off
    data object ClearCheck : GameAction                // sent by the VM 3 s after CheckBoard
}
