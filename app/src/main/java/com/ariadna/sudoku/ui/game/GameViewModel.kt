package com.ariadna.sudoku.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.core.game.GameSession
import com.ariadna.sudoku.core.game.GameState
import com.ariadna.sudoku.core.game.reduce
import com.ariadna.sudoku.data.game.GameRepository
import com.ariadna.sudoku.data.settings.SettingsRepository
import com.ariadna.sudoku.data.settings.UserSettings
import com.ariadna.sudoku.di.ApplicationScope
import com.ariadna.sudoku.di.DefaultDispatcher
import com.ariadna.sudoku.ui.navigation.GameKey
import com.ariadna.sudoku.util.ElapsedClock
import com.ariadna.sudoku.util.formatElapsed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted private val key: GameKey,
    private val games: GameRepository,
    settingsRepository: SettingsRepository,
    private val clock: ElapsedClock,
    @DefaultDispatcher default: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: GameKey): GameViewModel
    }

    private val game = MutableStateFlow<GameState?>(null)
    private val screen = MutableStateFlow(ScreenState())
    private val notFound = MutableStateFlow(false)
    private var screenResumed = false

    /** Null for untimed games (nothing is tracked). */
    private var timer: GameTimer? = null

    /** Null until a timed game is loaded; then whether its timer runs. Drives [timerText]. */
    private val timerRunning = MutableStateFlow<Boolean?>(null)

    private var clearCheckJob: Job? = null
    private var creatingNext = false

    private val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val uiState: StateFlow<GameUiState> = combine(game, settings, screen, notFound) { state, settings, screen, notFound ->
        when {
            notFound -> GameUiState.NotFound
            state == null -> GameUiState.Loading
            else -> GameUiMapper.map(state, settings, screen)
        }
    }
        .flowOn(default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState.Loading)

    /** The timer label, updated once per second while running, so only the label recomposes. Empty when untimed. */
    val timerText: StateFlow<String> = timerRunning
        .flatMapLatest { running ->
            val timer = timer
            when {
                running == null || timer == null -> flowOf("")
                !running -> flowOf(formatElapsed(timer.elapsedMs))
                else -> flow {
                    while (true) {
                        val elapsed = timer.elapsedMs
                        emit(formatElapsed(elapsed))
                        delay(1_000 - elapsed % 1_000)   // wake up on the next full second
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        viewModelScope.launch {
            val saved = games.load(key.gameId)
            if (saved == null) {
                notFound.value = true
                return@launch
            }
            if (saved.session.timed) timer = GameTimer(saved.elapsedMs, clock)
            if (saved.session.isSolved) screen.update { it.copy(solvedElapsedMs = saved.elapsedMs) }
            game.value = GameState(saved.session)
            syncTimer()
        }
        // Autosave: only real moves change `session`; selection, notes mode and so on never cause a write.
        viewModelScope.launch {
            game.filterNotNull()
                .map { it.session }
                .distinctUntilChanged()
                .drop(1)
                .debounce(AUTOSAVE_DEBOUNCE_MS)
                .collect { games.save(it, elapsedMs()) }
        }
    }

    fun onAction(action: GameScreenAction) {
        when (action) {
            is GameScreenAction.Game -> reduce(action.action)
            GameScreenAction.Pause -> {
                screen.update { it.copy(paused = true) }
                syncTimer()
                saveNow()
            }
            GameScreenAction.Resume -> {
                screen.update { it.copy(paused = false) }
                syncTimer()
            }
            GameScreenAction.ScreenResumed -> {
                screenResumed = true
                syncTimer()
            }
            GameScreenAction.ScreenPaused -> {
                screenResumed = false
                syncTimer()
                saveNow()
            }
            GameScreenAction.NextPuzzle -> startNextPuzzle()
            GameScreenAction.DismissCompletion -> screen.update { it.copy(completionDismissed = true) }
        }
    }

    private fun reduce(action: GameAction) {
        val before = game.value ?: return
        val after = before.reduce(action, settings.value.rules)
        game.value = after
        if (action == GameAction.CheckBoard && after.input.checkedWrongCells !== before.input.checkedWrongCells) {
            clearCheckJob?.cancel()
            clearCheckJob = viewModelScope.launch {
                delay(CHECK_DISPLAY_MS)
                reduce(GameAction.ClearCheck)
            }
        }
        if (!before.session.isSolved && after.session.isSolved) onSolved(after.session)
    }

    private fun onSolved(session: GameSession) {
        syncTimer()
        val elapsed = elapsedMs()
        screen.update { it.copy(solvedElapsedMs = elapsed) }
        viewModelScope.launch { games.save(session, elapsed) }
    }

    private fun startNextPuzzle() {
        val session = game.value?.session ?: return
        if (creatingNext || screen.value.nextGameId != null) return
        creatingNext = true
        viewModelScope.launch {
            val id = games.create(session.spec, session.difficulty, session.timed)
            screen.update { it.copy(nextGameId = id) }
        }
    }

    /** Runs the timer only while the game is in progress, not paused and the screen is resumed. */
    private fun syncTimer() {
        val timer = timer ?: return
        val session = game.value?.session ?: return
        if (!session.isSolved && !screen.value.paused && screenResumed) timer.start() else timer.stop()
        timerRunning.value = timer.isRunning
    }

    private fun elapsedMs(): Long = timer?.elapsedMs ?: 0

    private fun saveNow() {
        val session = game.value?.session ?: return
        val elapsed = elapsedMs()
        viewModelScope.launch { games.save(session, elapsed) }
    }

    override fun onCleared() {
        // viewModelScope is already cancelled here, so the final save runs in the application scope.
        val session = game.value?.session ?: return
        val elapsed = elapsedMs()
        appScope.launch { games.save(session, elapsed) }
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 500L
        const val CHECK_DISPLAY_MS = 3_000L
    }
}
