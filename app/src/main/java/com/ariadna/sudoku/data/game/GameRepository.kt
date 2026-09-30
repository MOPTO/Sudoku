package com.ariadna.sudoku.data.game

import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.GridSpec

/** A stored game and the time played on it (always 0 for untimed games). */
data class SavedGame(val session: GameSession, val elapsedMs: Long)

interface GameRepository {
    /** Generates a puzzle and stores a fresh game of it. Returns the new game's id. */
    suspend fun create(spec: GridSpec, difficulty: Difficulty, timed: Boolean): Long

    suspend fun load(id: Long): SavedGame?

    suspend fun save(session: GameSession, elapsedMs: Long)
}
