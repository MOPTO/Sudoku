package com.ariadna.sudoku.core.game

import kotlinx.serialization.Serializable

/** Value and notes of one cell before and after a [Move]. */
@Serializable
data class CellChange(
    val cell: Int,
    val oldValue: Int,
    val newValue: Int,
    val oldNotes: Int,
    val newNotes: Int,
)

/**
 * One undoable player action. [changes] holds every cell it touched, including peer notes cleared by
 * "auto-remove notes", so undo restores them too. [cell] is the cell the action was about (selected again
 * on undo/redo), or null for board-wide actions such as auto-notes.
 */
@Serializable
data class Move(val cell: Int?, val changes: List<CellChange>) {
    init {
        require(changes.isNotEmpty()) { "a move changes at least one cell" }
    }
}

/** Undo/redo stacks, most recent move last. Keeps at most [MAX_MOVES] undoable moves. */
@Serializable
data class History(
    val undo: List<Move> = emptyList(),
    val redo: List<Move> = emptyList(),
) {
    val canUndo: Boolean get() = undo.isNotEmpty()

    val canRedo: Boolean get() = redo.isNotEmpty()

    /** Records a new move: it becomes undoable and the redo stack is dropped. */
    fun push(move: Move): History = History(undo = (undo + move).takeLast(MAX_MOVES), redo = emptyList())

    /** The move to revert and the history after reverting it, or null if there is nothing to undo. */
    fun undo(): Pair<Move, History>? {
        val move = undo.lastOrNull() ?: return null
        return move to History(undo = undo.dropLast(1), redo = redo + move)
    }

    /** The move to re-apply and the history after re-applying it, or null if there is nothing to redo. */
    fun redo(): Pair<Move, History>? {
        val move = redo.lastOrNull() ?: return null
        return move to History(undo = undo + move, redo = redo.dropLast(1))
    }

    companion object {
        const val MAX_MOVES = 200
    }
}
