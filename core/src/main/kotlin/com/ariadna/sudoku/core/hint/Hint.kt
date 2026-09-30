package com.ariadna.sudoku.core.hint

import com.ariadna.sudoku.core.logic.Step as LogicStep

/**
 * A hint for the current board (§4.8), in the order the hint engine looks for them.
 * The engine itself arrives in milestone 10; the reducer already shows and reveals hints.
 */
sealed interface Hint {

    /** [cell] holds a digit that differs from the solution. */
    data class FixMistake(val cell: Int) : Hint

    /** The notes of [cell] are missing its true [digit]. */
    data class BadNote(val cell: Int, val digit: Int) : Hint

    /** The next logical deduction. */
    data class Step(val step: LogicStep) : Hint

    /** No implemented technique applies: reveal the solution digit of [cell]. */
    data class Reveal(val cell: Int) : Hint
}
