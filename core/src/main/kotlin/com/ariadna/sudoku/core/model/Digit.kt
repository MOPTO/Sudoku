package com.ariadna.sudoku.core.model

/**
 * Single-character digit encoding shared by grid strings, the number pad and keyboard input:
 * `0` = empty, `1`–`9`, then `A`–`G` for 10–16.
 */
object Digit {
    const val EMPTY = 0

    fun toChar(digit: Int): Char = when (digit) {
        EMPTY -> '0'
        in 1..9 -> '0' + digit
        in 10..GridSpec.MAX_SIZE -> 'A' + (digit - 10)
        else -> throw IllegalArgumentException("digit out of range: $digit")
    }

    /** Parses a digit char; `.` is also accepted as empty. Returns -1 for an unknown char. */
    fun fromChar(c: Char): Int = when (c) {
        '0', '.' -> EMPTY
        in '1'..'9' -> c - '0'
        in 'A'..'G' -> c - 'A' + 10
        in 'a'..'g' -> c - 'a' + 10
        else -> -1
    }
}
