package com.ariadna.sudoku.core.solver

import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.core.model.Houses
import kotlin.random.Random

/**
 * Backtracking solver over bitmask candidates, used as the uniqueness oracle.
 *
 * Each search node propagates naked and hidden singles to a fixpoint, then branches on the
 * empty cell with the fewest candidates (MRV).
 */
object BruteForceSolver {

    /** Number of solutions of [grid], counting stops at [limit]. Returns 0 for an invalid grid. */
    fun countSolutions(grid: Grid, limit: Int = 2): Int {
        require(limit >= 1) { "limit must be >= 1" }
        return Search(grid.spec, random = null, limit = limit).run(grid)
    }

    fun classify(grid: Grid): SolveResult = when (countSolutions(grid, limit = 2)) {
        0 -> SolveResult.NoSolution
        1 -> SolveResult.Unique
        else -> SolveResult.Multiple
    }

    /**
     * One solution of [grid], or null if it has none. With a [random], candidates are tried in
     * shuffled order, so solving an empty grid yields a random complete grid (deterministic per seed).
     */
    fun solve(grid: Grid, random: Random? = null): IntArray? {
        val search = Search(grid.spec, random, limit = 1)
        search.run(grid)
        return search.firstSolution
    }

    private class Search(spec: GridSpec, private val random: Random?, private val limit: Int) {
        private val houses = Houses.of(spec)
        private val size = spec.size
        private val cellCount = spec.cellCount
        private val full = Candidates.all(size).bits

        var count = 0
            private set
        var firstSolution: IntArray? = null
            private set

        fun run(grid: Grid): Int {
            val values = grid.values
            val cand = IntArray(cellCount)
            for (cell in 0 until cellCount) {
                val v = values[cell]
                if (v != 0) {
                    if (houses.peers[cell].any { values[it] == v }) return 0
                    continue
                }
                var used = 0
                for (peer in houses.peers[cell]) {
                    val pv = values[peer]
                    if (pv != 0) used = used or Candidates.bit(pv)
                }
                cand[cell] = full and used.inv()
            }
            search(values, cand)
            return count
        }

        private fun search(values: IntArray, cand: IntArray) {
            if (!propagate(values, cand)) return

            var best = -1
            var bestCount = Int.MAX_VALUE
            for (cell in 0 until cellCount) {
                if (values[cell] != 0) continue
                val c = cand[cell].countOneBits()
                if (c < bestCount) {
                    best = cell
                    bestCount = c
                    if (c == 2) break
                }
            }
            if (best == -1) {
                count++
                if (firstSolution == null) firstSolution = values.copyOf()
                return
            }

            val digits = Candidates(cand[best]).toList().let { if (random != null) it.shuffled(random) else it }
            for (d in digits) {
                val v = values.copyOf()
                val c = cand.copyOf()
                place(v, c, best, d)
                search(v, c)
                if (count >= limit) return
            }
        }

        /** Applies naked and hidden singles until nothing changes. Returns false on a contradiction. */
        private fun propagate(values: IntArray, cand: IntArray): Boolean {
            var changed = true
            while (changed) {
                changed = false

                // Naked singles.
                for (cell in 0 until cellCount) {
                    if (values[cell] != 0) continue
                    val c = cand[cell]
                    if (c == 0) return false
                    if (c and (c - 1) == 0) {
                        place(values, cand, cell, c.countTrailingZeroBits() + 1)
                        changed = true
                    }
                }

                // Hidden singles.
                for (house in houses.all) {
                    var once = 0
                    var twice = 0
                    var placed = 0
                    for (cell in house) {
                        val v = values[cell]
                        if (v != 0) {
                            placed = placed or Candidates.bit(v)
                        } else {
                            val c = cand[cell]
                            twice = twice or (once and c)
                            once = once or c
                        }
                    }
                    if ((once or placed) != full) return false   // some digit has no place in this house
                    var hidden = once and twice.inv() and placed.inv()
                    while (hidden != 0) {
                        val bit = hidden and -hidden
                        hidden = hidden xor bit
                        val target = house.firstOrNull { values[it] == 0 && cand[it] and bit != 0 } ?: return false
                        place(values, cand, target, bit.countTrailingZeroBits() + 1)
                        changed = true
                    }
                }
            }
            return true
        }

        private fun place(values: IntArray, cand: IntArray, cell: Int, digit: Int) {
            values[cell] = digit
            cand[cell] = 0
            val clear = Candidates.bit(digit).inv()
            for (peer in houses.peers[cell]) cand[peer] = cand[peer] and clear
        }
    }
}
