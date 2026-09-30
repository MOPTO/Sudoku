package com.ariadna.sudoku.core.logic

import com.ariadna.sudoku.core.model.Candidates

/** [digit] goes into [cell]. */
data class Placement(val cell: Int, val digit: Int)

/** [digit] is removed from the candidates of [cell]. */
data class Elimination(val cell: Int, val digit: Int)

/**
 * One logical deduction: either a [placement] or a non-empty list of [eliminations].
 *
 * [house] (index into `Houses.all`) is the house the deduction is about, [cells] the cells forming the
 * pattern and [digits] the digits involved; hints use them to point the player at the step.
 */
data class Step(
    val technique: Technique,
    val house: Int?,
    val cells: List<Int>,
    val digits: Candidates,
    val placement: Placement? = null,
    val eliminations: List<Elimination> = emptyList(),
) {
    init {
        require((placement != null) != eliminations.isNotEmpty()) { "a step places a digit or eliminates candidates" }
    }
}
