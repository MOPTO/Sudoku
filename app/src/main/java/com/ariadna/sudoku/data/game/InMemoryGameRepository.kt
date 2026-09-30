package com.ariadna.sudoku.data.game

import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.generator.PuzzleGenerator
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Keeps games in memory and generates puzzles on demand. Replaced by Room and the puzzle pool in
 * milestones 7 and 11; until then games do not survive process death.
 */
@Singleton
class InMemoryGameRepository @Inject constructor(
    private val random: Random,
    @DefaultDispatcher private val default: CoroutineDispatcher,
) : GameRepository {

    private val mutex = Mutex()
    private val games = HashMap<Long, SavedGame>()
    private var nextId = 1L

    override suspend fun create(spec: GridSpec, difficulty: Difficulty, timed: Boolean): Long {
        val puzzle = withContext(default) { PuzzleGenerator.generate(spec, difficulty, random.nextLong()).puzzle }
        return mutex.withLock {
            val id = nextId++
            games[id] = SavedGame(GameSession.start(id, puzzle, timed), elapsedMs = 0)
            id
        }
    }

    override suspend fun load(id: Long): SavedGame? = mutex.withLock { games[id] }

    override suspend fun save(session: GameSession, elapsedMs: Long) {
        mutex.withLock { games[session.id] = SavedGame(session, if (session.timed) elapsedMs else 0) }
    }
}
