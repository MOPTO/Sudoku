package com.ariadna.sudoku.core.model

import kotlinx.serialization.Serializable

/** Shape of a Sudoku: N×N cells, digits 1..N, boxes of [boxRows] × [boxCols]. */
@Serializable
data class GridSpec(
    val size: Int,
    val boxRows: Int,
    val boxCols: Int,
    val variant: Variant = Variant.Classic,
) {
    init {
        require(size in 4..MAX_SIZE) { "size must be in 4..$MAX_SIZE, was $size" }
        require(boxRows * boxCols == size) { "boxRows * boxCols must equal size ($boxRows * $boxCols != $size)" }
    }

    val cellCount: Int get() = size * size

    companion object {
        const val MAX_SIZE = 16

        val Mini4 = GridSpec(4, 2, 2)
        val Six = GridSpec(6, 2, 3)
        val Eight = GridSpec(8, 2, 4)
        val Classic = GridSpec(9, 3, 3)
        val Twelve = GridSpec(12, 3, 4)
        val Sixteen = GridSpec(16, 4, 4)
    }
}

@Serializable
enum class Variant { Classic, Diagonal /* X-Sudoku */, Windoku /* hyper */ }
