package com.ariadna.sudoku.ui.game

import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.core.game.GameAction.Input
import com.ariadna.sudoku.core.game.GameAction.SelectCell
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.GameStatus
import com.ariadna.sudoku.core.game.MistakeMode
import com.ariadna.sudoku.data.game.SavedGame
import com.ariadna.sudoku.data.settings.UserSettings
import com.ariadna.sudoku.ui.navigation.GameKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var repo: FakeGameRepository

    /** A ViewModel for game 1 whose `uiState` and `timerText` are collected, as the screen would. */
    private fun TestScope.viewModel(
        game: GameState?,
        elapsedMs: Long = 0,
        settings: UserSettings = UserSettings(),
    ): GameViewModel {
        repo = if (game == null) FakeGameRepository() else FakeGameRepository(SavedGame(game.session, elapsedMs))
        val vm = GameViewModel(
            key = GameKey(1),
            games = repo,
            settingsRepository = FakeSettingsRepository(settings),
            clock = { testScheduler.currentTime },
            default = mainRule.dispatcher,
            appScope = backgroundScope,
        )
        backgroundScope.launch { vm.uiState.collect {} }
        backgroundScope.launch { vm.timerText.collect {} }
        runCurrent()
        return vm
    }

    private val GameViewModel.ready: GameUiState.Ready get() = uiState.value as GameUiState.Ready

    private fun GameViewModel.play(vararg actions: GameAction) = actions.forEach { onAction(GameScreenAction.Game(it)) }

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `loads the game`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15))
        assertEquals(4, vm.ready.board.size)
    }

    @Test
    fun `a missing game reports NotFound`() = runTest {
        val vm = viewModel(null)
        assertEquals(GameUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun `autosave fires 500 ms after a move`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15))
        vm.play(SelectCell(0), Input(1))
        advance(499)
        assertTrue(repo.saves.isEmpty())
        advance(1)
        assertEquals(1, repo.saves.single().session.values[0])
    }

    @Test
    fun `selection and notes mode never trigger a save`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15))
        vm.play(SelectCell(0), GameAction.ToggleNotesMode, SelectCell(15), GameAction.ToggleNotesMode)
        advance(5_000)
        assertTrue(repo.saves.isEmpty())
    }

    @Test
    fun `the timer runs only while the screen is resumed`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15, timed = true), elapsedMs = 60_000)
        assertEquals("1:00", vm.timerText.value)
        advance(5_000)
        assertEquals("1:00", vm.timerText.value)   // not resumed yet

        vm.onAction(GameScreenAction.ScreenResumed)
        advance(5_000)
        assertEquals("1:05", vm.timerText.value)

        vm.onAction(GameScreenAction.ScreenPaused)
        runCurrent()
        assertEquals(65_000L, repo.saves.last().elapsedMs)   // leaving the screen saves at once
        advance(10_000)
        assertEquals("1:05", vm.timerText.value)

        vm.onAction(GameScreenAction.ScreenResumed)   // comes back running
        advance(2_000)
        assertEquals("1:07", vm.timerText.value)
    }

    @Test
    fun `a user pause survives the screen resuming`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15, timed = true))
        vm.onAction(GameScreenAction.ScreenResumed)
        advance(3_000)
        vm.onAction(GameScreenAction.Pause)
        runCurrent()
        assertTrue(vm.ready.paused)
        assertEquals(3_000L, repo.saves.last().elapsedMs)

        vm.onAction(GameScreenAction.ScreenPaused)
        vm.onAction(GameScreenAction.ScreenResumed)
        advance(10_000)
        assertEquals("0:03", vm.timerText.value)
        assertTrue(vm.ready.paused)

        vm.onAction(GameScreenAction.Resume)
        advance(1_000)
        assertEquals("0:04", vm.timerText.value)
    }

    @Test
    fun `untimed games track no time`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15))
        vm.onAction(GameScreenAction.ScreenResumed)
        advance(5_000)
        assertEquals("", vm.timerText.value)
        vm.onAction(GameScreenAction.ScreenPaused)
        runCurrent()
        assertEquals(0L, repo.saves.last().elapsedMs)
    }

    @Test
    fun `a check is cleared after 3 seconds`() = runTest {
        val vm = viewModel(TestGames.mini(0, 15), settings = UserSettings(mistakeMode = MistakeMode.Off))
        vm.play(SelectCell(0), Input(2), GameAction.CheckBoard)
        runCurrent()
        assertEquals(ValueState.Wrong, vm.ready.board.cells[0].valueState)
        advance(2_999)
        assertEquals(ValueState.Wrong, vm.ready.board.cells[0].valueState)
        advance(1)
        assertEquals(ValueState.Normal, vm.ready.board.cells[0].valueState)
    }

    @Test
    fun `solving stops the timer, saves and shows the completion dialog`() = runTest {
        val vm = viewModel(TestGames.mini(0, timed = true))
        vm.onAction(GameScreenAction.ScreenResumed)
        advance(3_000)
        vm.play(SelectCell(0), Input(1))
        runCurrent()

        assertEquals(3_000L, vm.ready.completion?.elapsedMs)
        val saved = repo.saves.last()
        assertEquals(GameStatus.Solved, saved.session.status)
        assertEquals(3_000L, saved.elapsedMs)
        advance(5_000)
        assertEquals("0:03", vm.timerText.value)

        vm.onAction(GameScreenAction.DismissCompletion)
        runCurrent()
        assertNull(vm.ready.completion)
    }

    @Test
    fun `next puzzle creates a game with the same settings`() = runTest {
        val vm = viewModel(TestGames.mini(0, timed = true))
        vm.play(SelectCell(0), Input(1))
        vm.onAction(GameScreenAction.NextPuzzle)
        vm.onAction(GameScreenAction.NextPuzzle)   // a double tap creates only one game
        runCurrent()

        assertNotNull(vm.ready.nextGameId)
        assertEquals(2L, vm.ready.nextGameId)
        val session = TestGames.mini(0).session
        assertEquals(listOf(Triple(session.spec, session.difficulty, true)), repo.created)
    }
}
