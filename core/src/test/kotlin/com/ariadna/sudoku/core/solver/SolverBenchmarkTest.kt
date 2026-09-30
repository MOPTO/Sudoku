package com.ariadna.sudoku.core.solver

import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.random.Random
import kotlin.test.Test

/** Logs solver timings. Not asserted: machine speed varies. Target (§4.2): 9×9 uniqueness check < 1 ms. */
class SolverBenchmarkTest {

    @Test
    fun uniquenessCheckTiming() {
        for ((spec, clues) in listOf(GridSpec.Six to 12, GridSpec.Classic to 28)) {
            val puzzles = (0L until 200L).map { seed -> randomPuzzle(spec, clues, Random(seed)) }
            puzzles.take(50).forEach { BruteForceSolver.countSolutions(it) }   // warm-up

            val nanos = puzzles.map { p ->
                val start = System.nanoTime()
                BruteForceSolver.countSolutions(p)
                System.nanoTime() - start
            }.sorted()
            val p50 = nanos[nanos.size / 2] / 1e6
            val p95 = nanos[nanos.size * 95 / 100] / 1e6
            println("countSolutions ${spec.size}x${spec.size} ($clues clues): p50=%.3f ms, p95=%.3f ms".format(p50, p95))
        }
    }

    /** Random full grid with all but [clues] cells blanked (not necessarily unique). */
    private fun randomPuzzle(spec: GridSpec, clues: Int, random: Random): Grid {
        val values = BruteForceSolver.solve(Grid.empty(spec), random)!!
        (0 until spec.cellCount).shuffled(random).drop(clues).forEach { values[it] = 0 }
        return Grid.ofGivens(spec, values)
    }
}
