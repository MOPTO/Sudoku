package com.ariadna.sudoku.core.grading

import kotlinx.serialization.Serializable

/** Difficulty labels offered in the New-game sheet. `Master` arrives with the T5/T6 techniques (milestone 14). */
@Serializable
enum class Difficulty { Easy, Medium, Hard, Expert }
