package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.Houses

/**
 * Working state of the [LogicalSolver]: cell values (0 = empty) plus the remaining candidates of each
 * empty cell as bitmasks. Only the solver changes it; the [Grid] it was built from is never touched.
 */
class LogicBoard private constructor(val spec: GridSpec, private val values: IntArray, private val cand: IntArray) {

    val houses: Houses = Houses.of(spec)

    fun value(cell: Int): Int = values[cell]

    /** Remaining candidates of [cell] (empty for a filled cell). */
    fun candidates(cell: Int): Candidates = Candidates(cand[cell])

    internal fun candidateBits(cell: Int): Int = cand[cell]

    val isSolved: Boolean get() = values.none { it == 0 }

    /** Copy of the current values. */
    fun values(): IntArray = values.copyOf()

    internal fun apply(step: Step) {
        step.placement?.let { place(it.cell, it.digit) }
        for (e in step.eliminations) cand[e.cell] = cand[e.cell] and Candidates.bit(e.digit).inv()
    }

    /** Replaces the candidates of an empty [cell]; test fixtures use it to build exact candidate states. */
    internal fun setCandidates(cell: Int, candidates: Candidates) {
        require(values[cell] == 0) { "cell $cell is filled" }
        cand[cell] = candidates.bits
    }

    private fun place(cell: Int, digit: Int) {
        values[cell] = digit
        cand[cell] = 0
        val clear = Candidates.bit(digit).inv()
        for (peer in houses.peers[cell]) cand[peer] = cand[peer] and clear
    }

    companion object {
        /** Board with the values of [grid] and every candidate its filled peers allow. */
        fun of(grid: Grid): LogicBoard = of(grid.spec, grid.values)

        fun of(spec: GridSpec, values: IntArray): LogicBoard {
            require(values.size == spec.cellCount) { "expected ${spec.cellCount} values" }
            val houses = Houses.of(spec)
            val full = Candidates.all(spec.size).bits
            val cand = IntArray(spec.cellCount) { cell ->
                if (values[cell] != 0) {
                    0
                } else {
                    var used = 0
                    for (peer in houses.peers[cell]) if (values[peer] != 0) used = used or Candidates.bit(values[peer])
                    full and used.inv()
                }
            }
            return LogicBoard(spec, values.copyOf(), cand)
        }
    }
}
