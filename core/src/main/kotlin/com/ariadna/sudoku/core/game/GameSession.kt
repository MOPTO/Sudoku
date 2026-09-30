package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.generator.Puzzle
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import kotlinx.serialization.Serializable

@Serializable
enum class GameStatus { InProgress, Solved }

/**
 * Everything that is persisted about a game (§4.9). Changes only on real moves, so selection or notes mode
 * never cause a save.
 *
 * The arrays are owned by the session and never mutated: the reducer copies before it changes anything.
 * Equality compares array contents.
 */
data class GameSession(
    val id: Long,
    val spec: GridSpec,
    val difficulty: Difficulty,
    val seed: Long,
    /** Chosen in the New-game sheet, fixed for this game. */
    val timed: Boolean,
    /** The puzzle; every non-empty cell is a given. */
    val givens: Grid,
    val solution: IntArray,
    /** The player's values, givens included (0 = empty). */
    val values: IntArray,
    /** Candidates bits per cell (pencil marks); always 0 on filled cells. */
    val notes: IntArray,
    /** Counted only in [MistakeMode.Immediate]. */
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val history: History = History(),
    val status: GameStatus = GameStatus.InProgress,
) {
    init {
        require(givens.spec == spec) { "givens spec ${givens.spec} != $spec" }
        require(solution.size == spec.cellCount) { "expected ${spec.cellCount} solution values" }
        require(values.size == spec.cellCount) { "expected ${spec.cellCount} values" }
        require(notes.size == spec.cellCount) { "expected ${spec.cellCount} notes" }
    }

    fun isGiven(cell: Int): Boolean = givens.isGiven(cell)

    val isSolved: Boolean get() = status == GameStatus.Solved

    override fun equals(other: Any?): Boolean =
        other is GameSession && id == other.id && spec == other.spec && difficulty == other.difficulty &&
            seed == other.seed && timed == other.timed && givens == other.givens &&
            solution.contentEquals(other.solution) && values.contentEquals(other.values) &&
            notes.contentEquals(other.notes) && mistakes == other.mistakes && hintsUsed == other.hintsUsed &&
            history == other.history && status == other.status

    override fun hashCode(): Int {
        var h = id.hashCode()
        h = 31 * h + spec.hashCode()
        h = 31 * h + difficulty.hashCode()
        h = 31 * h + seed.hashCode()
        h = 31 * h + timed.hashCode()
        h = 31 * h + givens.hashCode()
        h = 31 * h + solution.contentHashCode()
        h = 31 * h + values.contentHashCode()
        h = 31 * h + notes.contentHashCode()
        h = 31 * h + mistakes
        h = 31 * h + hintsUsed
        h = 31 * h + history.hashCode()
        return 31 * h + status.hashCode()
    }

    override fun toString(): String =
        "GameSession(id=$id, ${spec.size}x${spec.size}, $difficulty, status=$status, mistakes=$mistakes, " +
            "hintsUsed=$hintsUsed, values=${Grid(spec, values, givens.givens).encode()})"

    companion object {
        /** A fresh game of [puzzle]: only the givens are filled, no notes, no history. */
        fun start(id: Long, puzzle: Puzzle, timed: Boolean): GameSession = GameSession(
            id = id,
            spec = puzzle.spec,
            difficulty = puzzle.difficulty,
            seed = puzzle.seed,
            timed = timed,
            givens = puzzle.givens,
            solution = puzzle.solution,
            values = puzzle.givens.values,
            notes = IntArray(puzzle.spec.cellCount),
        )
    }
}
