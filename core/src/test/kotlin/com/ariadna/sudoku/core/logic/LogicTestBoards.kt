package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec

/**
 * Builds exact candidate states for technique fixtures. An empty board has every candidate everywhere,
 * so no technique applies until a fixture removes candidates to create its pattern.
 */
object LogicTestBoards {

    fun empty(spec: GridSpec = GridSpec.Classic): LogicBoard = LogicBoard.of(Grid.empty(spec))

    fun of(spec: GridSpec, values: String): LogicBoard = LogicBoard.of(spec, Grid.decodeValues(spec, values))

    /** Removes the [digits] from the candidates of every cell in [cells]. */
    fun LogicBoard.remove(cells: Iterable<Int>, vararg digits: Int): LogicBoard = apply {
        val mask = Candidates.of(*digits).bits
        for (cell in cells) setCandidates(cell, Candidates(candidateBits(cell) and mask.inv()))
    }

    /** Leaves only [digits] as candidates of [cell]. */
    fun LogicBoard.only(cell: Int, vararg digits: Int): LogicBoard = apply { setCandidates(cell, Candidates.of(*digits)) }

    fun eliminations(cells: Iterable<Int>, digit: Int): List<Elimination> = cells.map { Elimination(it, digit) }
}
