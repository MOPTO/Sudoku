package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.grading.Grade
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec

/** A generated puzzle, reproducible from ([spec], [difficulty], [seed], [generatorVersion]). */
class Puzzle(
    val spec: GridSpec,
    val difficulty: Difficulty,
    /** Seed of the attempt that produced this puzzle. */
    val seed: Long,
    val givens: Grid,
    solution: IntArray,
    /** Grader result for [givens]. */
    val grade: Grade,
    val generatorVersion: Int = GENERATOR_VERSION,
) {
    private val solutionValues: IntArray = solution.copyOf()

    init {
        require(givens.spec == spec) { "givens spec ${givens.spec} != $spec" }
        require(solutionValues.size == spec.cellCount) { "expected ${spec.cellCount} solution values" }
    }

    val solution: IntArray get() = solutionValues.copyOf()

    val clueCount: Int get() = givens.filledCount

    override fun equals(other: Any?): Boolean =
        other is Puzzle && spec == other.spec && difficulty == other.difficulty && seed == other.seed &&
            givens == other.givens && solutionValues.contentEquals(other.solutionValues) && grade == other.grade &&
            generatorVersion == other.generatorVersion

    override fun hashCode(): Int {
        var h = spec.hashCode()
        h = 31 * h + difficulty.hashCode()
        h = 31 * h + seed.hashCode()
        h = 31 * h + givens.hashCode()
        h = 31 * h + solutionValues.contentHashCode()
        h = 31 * h + grade.hashCode()
        return 31 * h + generatorVersion
    }

    override fun toString(): String =
        "Puzzle(${spec.size}x${spec.size}, $difficulty, seed=$seed, clues=$clueCount, ${grade.tier}/${grade.score}, ${givens.encode()})"
}
