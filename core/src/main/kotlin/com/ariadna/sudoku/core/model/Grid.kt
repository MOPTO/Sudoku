package com.ariadna.sudoku.core.model

/**
 * Cell values (0 = empty) plus a flag per cell telling whether it is a given.
 * Arrays are never shared: the constructor and accessors copy, so a [Grid] is effectively immutable.
 */
class Grid(val spec: GridSpec, values: IntArray, givens: BooleanArray) {

    private val cells: IntArray = values.copyOf()
    private val givenFlags: BooleanArray = givens.copyOf()

    init {
        require(cells.size == spec.cellCount) { "expected ${spec.cellCount} values, got ${cells.size}" }
        require(givenFlags.size == spec.cellCount) { "expected ${spec.cellCount} given flags, got ${givenFlags.size}" }
        cells.forEach { require(it in 0..spec.size) { "value out of range for size ${spec.size}: $it" } }
    }

    val values: IntArray get() = cells.copyOf()

    val givens: BooleanArray get() = givenFlags.copyOf()

    operator fun get(index: Int): Int = cells[index]

    fun isGiven(index: Int): Boolean = givenFlags[index]

    val filledCount: Int get() = cells.count { it != 0 }

    val isComplete: Boolean get() = cells.none { it == 0 }

    /** One char per cell, row-major (see [Digit]). */
    fun encode(): String = buildString(cells.size) { cells.forEach { append(Digit.toChar(it)) } }

    override fun equals(other: Any?): Boolean =
        other is Grid && spec == other.spec &&
            cells.contentEquals(other.cells) && givenFlags.contentEquals(other.givenFlags)

    override fun hashCode(): Int =
        31 * (31 * spec.hashCode() + cells.contentHashCode()) + givenFlags.contentHashCode()

    override fun toString(): String = "Grid(${spec.size}x${spec.size}, ${encode()})"

    companion object {
        fun empty(spec: GridSpec): Grid = Grid(spec, IntArray(spec.cellCount), BooleanArray(spec.cellCount))

        /** Grid whose non-empty cells are all givens (a puzzle). */
        fun ofGivens(spec: GridSpec, values: IntArray): Grid =
            Grid(spec, values, BooleanArray(values.size) { values[it] != 0 })

        /** Decodes [encoded] (from [encode]); every non-empty cell becomes a given. */
        fun parse(spec: GridSpec, encoded: String): Grid = ofGivens(spec, decodeValues(spec, encoded))

        /** Decodes a grid string into raw values without given flags. */
        fun decodeValues(spec: GridSpec, encoded: String): IntArray {
            require(encoded.length == spec.cellCount) {
                "expected ${spec.cellCount} chars, got ${encoded.length}"
            }
            return IntArray(encoded.length) { i ->
                val d = Digit.fromChar(encoded[i])
                require(d in 0..spec.size) { "invalid char '${encoded[i]}' at $i for size ${spec.size}" }
                d
            }
        }
    }
}
