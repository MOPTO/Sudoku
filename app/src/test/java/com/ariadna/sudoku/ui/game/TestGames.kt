package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.core.game.GameRules
import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.reduce
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.Grid
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.data.game.GameRepository
import com.ariadna.sudoku.data.game.SavedGame
import com.ariadna.sudoku.data.settings.SettingsRepository
import com.ariadna.sudoku.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Game fixtures for the app's unit tests. */
object TestGames {

    // Row 0 of the classic puzzle is "53__7____" (solution 534678912); cell 19 is a given 9 in box 0,
    // cell 14 a given 5 outside cell 0's houses.
    const val CLASSIC_PUZZLE = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    const val CLASSIC_SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    /** Solution rows 1234 / 3412 / 2143 / 4321: cells 0 and 15 hold the digit 1. */
    const val MINI_SOLUTION = "1234341221434321"

    fun classic(timed: Boolean = false): GameState =
        state(GridSpec.Classic, CLASSIC_PUZZLE, CLASSIC_SOLUTION, timed)

    /** 4×4 board that is solved except for [blanks]. */
    fun mini(vararg blanks: Int, timed: Boolean = false): GameState {
        val puzzle = MINI_SOLUTION.toCharArray().also { chars -> blanks.forEach { chars[it] = '0' } }
        return state(GridSpec.Mini4, String(puzzle), MINI_SOLUTION, timed)
    }

    fun state(spec: GridSpec, puzzle: String, solution: String, timed: Boolean = false): GameState {
        val givens = Grid.parse(spec, puzzle)
        return GameState(
            GameSession(
                id = 1,
                spec = spec,
                difficulty = Difficulty.Easy,
                seed = 0,
                timed = timed,
                givens = givens,
                solution = Grid.decodeValues(spec, solution),
                values = givens.values,
                notes = IntArray(spec.cellCount),
            ),
        )
    }

    fun GameState.act(vararg actions: GameAction, rules: GameRules = GameRules()): GameState =
        actions.fold(this) { state, action -> state.reduce(action, rules) }
}

/** Stores games in a map and records every save. `create` copies [template] under a new id. */
class FakeGameRepository(vararg initial: SavedGame) : GameRepository {
    val games = initial.associateBy { it.session.id }.toMutableMap()
    val saves = mutableListOf<SavedGame>()
    val created = mutableListOf<Triple<GridSpec, Difficulty, Boolean>>()
    var template: GameSession? = initial.firstOrNull()?.session

    override suspend fun create(spec: GridSpec, difficulty: Difficulty, timed: Boolean): Long {
        created += Triple(spec, difficulty, timed)
        val id = (games.keys.maxOrNull() ?: 0) + 1
        games[id] = SavedGame(checkNotNull(template).copy(id = id, timed = timed), 0)
        return id
    }

    override suspend fun load(id: Long): SavedGame? = games[id]

    override suspend fun save(session: GameSession, elapsedMs: Long) {
        val saved = SavedGame(session, elapsedMs)
        games[session.id] = saved
        saves += saved
    }
}

class FakeSettingsRepository(settings: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(settings)
}

/** Replaces Dispatchers.Main with a [StandardTestDispatcher]; `runTest` then shares its scheduler. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
