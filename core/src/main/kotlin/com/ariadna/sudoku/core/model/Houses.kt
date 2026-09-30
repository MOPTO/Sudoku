package com.ariadna.sudoku.core.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Houses (units) of a [GridSpec]: every group of cells that must contain each digit exactly once.
 * Solvers, techniques, highlighting and conflict checks all work over [all] and [peers],
 * so variants only need to add houses here.
 */
class Houses private constructor(val spec: GridSpec) {

    /** Every house as a list of cell indices: rows, then columns, then boxes (then variant houses). */
    val all: List<IntArray>

    /** Cell index → indices into [all] of the houses containing that cell. */
    val housesOf: Array<IntArray>

    /** Cell index → distinct peer cells (cells sharing a house), sorted, excluding the cell itself. */
    val peers: Array<IntArray>

    init {
        val n = spec.size
        val houses = ArrayList<IntArray>()
        for (r in 0 until n) houses += IntArray(n) { c -> spec.index(r, c) }
        for (c in 0 until n) houses += IntArray(n) { r -> spec.index(r, c) }
        for (b in 0 until n) {
            val top = (b / spec.boxRows) * spec.boxRows
            val left = (b % spec.boxRows) * spec.boxCols
            houses += IntArray(n) { i -> spec.index(top + i / spec.boxCols, left + i % spec.boxCols) }
        }
        when (spec.variant) {
            Variant.Classic -> Unit
            Variant.Diagonal, Variant.Windoku ->
                throw NotImplementedError("${spec.variant} houses are planned for milestone 15")
        }
        all = houses

        val housesPerCell = Array(spec.cellCount) { ArrayList<Int>(3) }
        houses.forEachIndexed { h, cells -> cells.forEach { housesPerCell[it] += h } }
        housesOf = Array(spec.cellCount) { housesPerCell[it].toIntArray() }

        peers = Array(spec.cellCount) { cell ->
            val set = java.util.TreeSet<Int>()
            housesOf[cell].forEach { h -> houses[h].forEach { set += it } }
            set -= cell
            set.toIntArray()
        }
    }

    companion object {
        private val cache = ConcurrentHashMap<GridSpec, Houses>()

        /** Houses for [spec], computed once and cached. */
        fun of(spec: GridSpec): Houses = cache.getOrPut(spec) { Houses(spec) }
    }
}
