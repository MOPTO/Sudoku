package com.ariadna.sudoku.core.model

// Cells are indexed row-major: index = row * size + col.

fun GridSpec.row(index: Int): Int = index / size

fun GridSpec.col(index: Int): Int = index % size

/** Box number, row-major over boxes (each band of boxes holds `size / boxCols` boxes). */
fun GridSpec.box(index: Int): Int = (row(index) / boxRows) * (size / boxCols) + col(index) / boxCols

fun GridSpec.index(row: Int, col: Int): Int = row * size + col
