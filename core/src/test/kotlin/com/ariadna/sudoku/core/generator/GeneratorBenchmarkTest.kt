package com.ariadna.sudoku.core.generator

import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/** Logs generation timings per (spec, difficulty). Not asserted: machine speed varies. */
class GeneratorBenchmarkTest {

    @Test
    fun generationTiming() = runBlocking {
        for (spec in listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)) {
            for (difficulty in DifficultyProfile.supported(spec)) {
                repeat(20) { PuzzleGenerator.generate(spec, difficulty, seed = 1_000L + it) }   // warm-up

                val runs = (0L until 100L).map { seed ->
                    val start = System.nanoTime()
                    val result = PuzzleGenerator.generate(spec, difficulty, seed)
                    (System.nanoTime() - start) to result
                }
                val nanos = runs.map { it.first }.sorted()
                val results = runs.map { it.second }
                println(
                    "generate %dx%d %-6s p50=%.2f ms p95=%.2f ms attempts=%.2f inRange=%d%% clues=%.1f score=%.0f tiers=%s".format(
                        spec.size, spec.size, difficulty,
                        nanos[nanos.size / 2] / 1e6, nanos[nanos.size * 95 / 100] / 1e6,
                        results.map { it.attempts }.average(),
                        results.count { it.inRange } * 100 / results.size,
                        results.map { it.puzzle.clueCount }.average(),
                        results.map { it.puzzle.grade.score }.average(),
                        results.groupingBy { it.puzzle.grade.tier }.eachCount().toSortedMap(),
                    ),
                )
            }
        }
    }
}
