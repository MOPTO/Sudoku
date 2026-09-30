package com.ariadna.sudoku.core

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.Houses

/** Shared fixtures and checks for :core tests. */
object TestGrids {

    // Wikipedia's example puzzle and its (unique) solution.
    const val CLASSIC_PUZZLE =
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    const val CLASSIC_SOLUTION =
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    const val SIX_SOLUTION = "123456456123231564564231312645645312"

    const val MINI_SOLUTION = "1234341221434321"

    /** True if [values] is a complete grid where every house holds each digit 1..N once. */
    fun isValidSolution(spec: GridSpec, values: IntArray): Boolean {
        if (values.size != spec.cellCount || values.any { it !in 1..spec.size }) return false
        val full = Candidates.all(spec.size).bits
        return Houses.of(spec).all.all { house ->
            house.fold(0) { acc, cell -> acc or Candidates.bit(values[cell]) } == full
        }
    }

    /** True if [solution] keeps every non-empty cell of [puzzle]. */
    fun respectsGivens(puzzle: IntArray, solution: IntArray): Boolean =
        puzzle.indices.all { puzzle[it] == 0 || puzzle[it] == solution[it] }
}
