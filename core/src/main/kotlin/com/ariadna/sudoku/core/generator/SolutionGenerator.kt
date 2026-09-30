package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.index
import com.ariadna.sudoku.core.solver.BruteForceSolver
import kotlin.random.Random

/** Random complete, valid grids. Deterministic for a given [Random] state. */
object SolutionGenerator {

    fun generate(spec: GridSpec, random: Random): IntArray {
        val values = IntArray(spec.cellCount)
        // With square boxes the diagonal boxes share no row, column or box. From 3×3 boxes up any
        // fill of them completes (not so for 4×4), and filling them first saves most of the branching.
        if (spec.boxRows == spec.boxCols && spec.boxRows >= 3) {
            for (b in 0 until spec.size / spec.boxCols) {
                val digits = (1..spec.size).shuffled(random)
                for (i in 0 until spec.size) {
                    val row = b * spec.boxRows + i / spec.boxCols
                    val col = b * spec.boxCols + i % spec.boxCols
                    values[spec.index(row, col)] = digits[i]
                }
            }
        }
        return BruteForceSolver.solve(Grid.ofGivens(spec, values), random)
            ?: checkNotNull(BruteForceSolver.solve(Grid.empty(spec), random))
    }
}
