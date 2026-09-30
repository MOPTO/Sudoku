package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameAction.AdvanceHint
import com.ariadna.sudoku.core.game.GameAction.CheckBoard
import com.ariadna.sudoku.core.game.GameAction.ClearCheck
import com.ariadna.sudoku.core.game.GameAction.DismissHint
import com.ariadna.sudoku.core.game.GameAction.Erase
import com.ariadna.sudoku.core.game.GameAction.FillAllNotes
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.LockDigit
import com.ariadna.sudoku.core.game.GameAction.MoveSelection
import com.ariadna.sudoku.core.game.GameAction.Redo
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameAction.ShowHint
import com.ariadna.sudoku.core.game.GameAction.ToggleNotesMode
import com.ariadna.sudoku.core.game.GameAction.Undo
import com.ariadna.sudoku.core.hint.Hint
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.col
import com.ariadna.sudoku.core.model.index
import com.ariadna.sudoku.core.model.row

/**
 * The game's pure reducer (§4.9). It never mutates arrays and never runs solver logic.
 *
 * Returns the same instance when an action changes nothing. Input-only actions keep the same `session`
 * reference, so the autosave (which watches `session`) is not triggered by them. Once the game is solved,
 * every action that would change the session is ignored.
 */
fun GameState.reduce(action: GameAction, rules: GameRules): GameState = when (action) {
    is SelectCell -> selectCell(action.index, rules)
    is MoveSelection -> moveSelection(action.dRow, action.dCol)
    is Input -> input.selected?.let { inputAt(it, action.digit, rules) } ?: this
    Erase -> input.selected?.let { cell -> eraseMove(cell)?.let { record(it) } } ?: this
    ToggleNotesMode -> copy(input = input.copy(notesMode = !input.notesMode))
    is LockDigit -> lockDigit(action.digit)
    Undo -> undo()
    Redo -> redo()
    FillAllNotes -> fillAllNotes()
    is ShowHint -> showHint(action.hint)
    AdvanceHint -> advanceHint(rules)
    DismissHint -> if (input.hint == null) this else copy(input = input.copy(hint = null))
    CheckBoard -> checkBoard(rules)
    ClearCheck -> if (input.checkedWrongCells.isEmpty()) this else copy(input = input.copy(checkedWrongCells = emptySet()))
}

private fun GameState.selectCell(cell: Int, rules: GameRules): GameState {
    if (cell !in 0 until session.spec.cellCount) return this
    val selected = if (input.selected == cell) this else copy(input = input.copy(selected = cell))
    // Digit-first mode: tapping a cell enters the locked digit.
    return input.lockedDigit?.let { selected.inputAt(cell, it, rules) } ?: selected
}

private fun GameState.moveSelection(dRow: Int, dCol: Int): GameState {
    val spec = session.spec
    val target = input.selected?.let { from ->
        spec.index((spec.row(from) + dRow).coerceIn(0, spec.size - 1), (spec.col(from) + dCol).coerceIn(0, spec.size - 1))
    } ?: 0
    return if (target == input.selected) this else copy(input = input.copy(selected = target))
}

private fun GameState.lockDigit(digit: Int?): GameState {
    if (digit != null && digit !in 1..session.spec.size) return this
    return if (digit == input.lockedDigit) this else copy(input = input.copy(lockedDigit = digit))
}

/** Enters [digit] into [cell]: a note in notes mode, otherwise a value (entering the cell's own value erases it). */
private fun GameState.inputAt(cell: Int, digit: Int, rules: GameRules): GameState {
    if (session.isSolved || session.isGiven(cell) || digit !in 1..session.spec.size) return this
    val value = session.values[cell]
    if (input.notesMode) {
        if (value != 0) return this
        val notes = session.notes[cell]
        return record(Move(cell, listOf(CellChange(cell, 0, 0, notes, notes xor Candidates.bit(digit)))))
    }
    if (value == digit) return eraseMove(cell)?.let { record(it) } ?: this
    val wrong = rules.mistakeMode == MistakeMode.Immediate && digit != session.solution[cell]
    return record(placeMove(cell, digit, rules), mistakes = session.mistakes + if (wrong) 1 else 0)
}

/** Places [digit] in [cell], clears the cell's notes and, with auto-remove, the digit from its peers' notes. */
private fun GameState.placeMove(cell: Int, digit: Int, rules: GameRules): Move {
    val own = CellChange(cell, session.values[cell], digit, session.notes[cell], 0)
    val peers = if (rules.autoRemoveNotes) {
        NotesOps.removeFromPeers(session.spec, session.values, session.notes, cell, digit)
    } else {
        emptyList()
    }
    return Move(cell, listOf(own) + peers)
}

/** Clears the value of [cell], or its notes if it is empty. Null if there is nothing to erase. */
private fun GameState.eraseMove(cell: Int): Move? {
    if (session.isSolved || session.isGiven(cell)) return null
    val value = session.values[cell]
    val notes = session.notes[cell]
    return when {
        value != 0 -> Move(cell, listOf(CellChange(cell, value, 0, notes, 0)))
        notes != 0 -> Move(cell, listOf(CellChange(cell, 0, 0, notes, 0)))
        else -> null
    }
}

private fun GameState.fillAllNotes(): GameState {
    if (session.isSolved) return this
    val changes = NotesOps.fillAll(session.spec, session.values, session.notes)
    return if (changes.isEmpty()) this else record(Move(null, changes))
}

private fun GameState.undo(): GameState {
    if (session.isSolved) return this
    val (move, history) = session.history.undo() ?: return this
    return applyMove(move, forward = false, history = history, selected = move.cell ?: input.selected)
}

private fun GameState.redo(): GameState {
    if (session.isSolved) return this
    val (move, history) = session.history.redo() ?: return this
    return applyMove(move, forward = true, history = history, selected = move.cell ?: input.selected)
}

private fun GameState.showHint(hint: Hint): GameState {
    if (session.isSolved) return this
    return copy(session = session.copy(hintsUsed = session.hintsUsed + 1), input = input.copy(hint = HintState(hint)))
}

/** Reveals the next hint level; reaching the last level applies the hint to the board as a normal move. */
private fun GameState.advanceHint(rules: GameRules): GameState {
    val shown = input.hint ?: return this
    if (session.isSolved || shown.level == HintState.MAX_LEVEL) return this
    val next = shown.copy(level = shown.level + 1)
    val hintsUsed = session.hintsUsed + 1
    val move = if (next.level == HintState.MAX_LEVEL) revealMove(shown.hint, rules) else null
    return if (move == null) {
        copy(session = session.copy(hintsUsed = hintsUsed), input = input.copy(hint = next))
    } else {
        record(move, hintsUsed = hintsUsed, hint = next)
    }
}

/** The move that carries out [hint], or null if the board no longer matches it (the hint went stale). */
private fun GameState.revealMove(hint: Hint, rules: GameRules): Move? {
    val values = session.values
    val notes = session.notes
    return when (hint) {
        is Hint.FixMistake -> {
            val cell = hint.cell
            if (session.isGiven(cell) || values[cell] == 0 || values[cell] == session.solution[cell]) null
            else placeMove(cell, session.solution[cell], rules)
        }
        is Hint.Reveal ->
            if (values[hint.cell] != 0) null else placeMove(hint.cell, session.solution[hint.cell], rules)
        is Hint.BadNote -> {
            val cell = hint.cell
            if (values[cell] != 0 || hint.digit in Candidates(notes[cell])) null
            else Move(cell, listOf(CellChange(cell, 0, 0, notes[cell], notes[cell] or Candidates.bit(hint.digit))))
        }
        is Hint.Step -> {
            val placement = hint.step.placement
            if (placement != null) {
                if (values[placement.cell] != 0) null else placeMove(placement.cell, placement.digit, rules)
            } else {
                // Several eliminations may hit the same cell: combine them into one change per cell.
                val newNotes = LinkedHashMap<Int, Int>()
                for (e in hint.step.eliminations) {
                    val current = newNotes[e.cell] ?: notes[e.cell]
                    if (values[e.cell] == 0 && e.digit in Candidates(current)) {
                        newNotes[e.cell] = current and Candidates.bit(e.digit).inv()
                    }
                }
                val changes = newNotes.map { (cell, n) -> CellChange(cell, 0, 0, notes[cell], n) }
                if (changes.isEmpty()) null else Move(null, changes)
            }
        }
    }
}

private fun GameState.checkBoard(rules: GameRules): GameState {
    if (session.isSolved || rules.mistakeMode != MistakeMode.Off) return this
    return copy(
        session = session.copy(hintsUsed = session.hintsUsed + 1),
        input = input.copy(checkedWrongCells = MistakeChecker.wrongCells(session)),
    )
}

/** Applies a new [move] and makes it undoable. Any shown hint is dismissed unless [hint] keeps one. */
private fun GameState.record(
    move: Move,
    mistakes: Int = session.mistakes,
    hintsUsed: Int = session.hintsUsed,
    hint: HintState? = null,
): GameState = applyMove(move, forward = true, history = session.history.push(move), mistakes = mistakes, hintsUsed = hintsUsed, hint = hint)

/**
 * Writes the new ([forward]) or old side of [move] into copies of the board, then runs the solved check.
 * Cells whose value changed lose their Check mark.
 */
private fun GameState.applyMove(
    move: Move,
    forward: Boolean,
    history: History,
    mistakes: Int = session.mistakes,
    hintsUsed: Int = session.hintsUsed,
    selected: Int? = input.selected,
    hint: HintState? = null,
): GameState {
    val values = session.values.copyOf()
    val notes = session.notes.copyOf()
    val changedValues = HashSet<Int>()
    for (c in move.changes) {
        values[c.cell] = if (forward) c.newValue else c.oldValue
        notes[c.cell] = if (forward) c.newNotes else c.oldNotes
        if (c.oldValue != c.newValue) changedValues += c.cell
    }
    val solved = values.contentEquals(session.solution)
    return GameState(
        session = session.copy(
            values = values,
            notes = notes,
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            history = history,
            status = if (solved) GameStatus.Solved else GameStatus.InProgress,
        ),
        input = input.copy(
            selected = selected,
            hint = if (solved) null else hint,
            checkedWrongCells = if (solved) emptySet() else input.checkedWrongCells - changedValues,
        ),
    )
}
