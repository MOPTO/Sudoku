package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.TestGrids.isValidSolution
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class SolutionGeneratorTest {

    private val specs = listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)

    @Test
    fun producesValidGrids() {
        for (spec in specs) {
            for (seed in 0L until 100L) {
                val values = SolutionGenerator.generate(spec, Random(seed))
                assertTrue(isValidSolution(spec, values), "${spec.size}x${spec.size} seed $seed")
            }
        }
    }

    @Test
    fun otherSizesWork() {
        for (spec in listOf(GridSpec.Eight, GridSpec.Twelve)) {
            for (seed in 0L until 5L) {
                assertTrue(isValidSolution(spec, SolutionGenerator.generate(spec, Random(seed))))
            }
        }
    }

    @Test
    fun sameSeedSameGrid() {
        for (spec in specs) {
            assertContentEquals(
                SolutionGenerator.generate(spec, Random(42)),
                SolutionGenerator.generate(spec, Random(42)),
            )
        }
    }

    @Test
    fun differentSeedsGiveVariety() {
        for (spec in specs) {
            val distinct = (0L until 20L).map { SolutionGenerator.generate(spec, Random(it)).toList() }.toSet()
            // 4×4 has only 288 solutions, so allow a few collisions there.
            assertTrue(distinct.size >= 15, "${spec.size}x${spec.size}: only ${distinct.size} distinct grids")
        }
    }
}
