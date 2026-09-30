package com.ariadna.sudoku.core.logic

/** Finds the first application of one [technique] on a board, or null. Never changes the board. */
internal interface TechniqueFinder {
    val technique: Technique

    fun find(board: LogicBoard): Step?
}
