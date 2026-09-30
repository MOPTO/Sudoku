package com.ariadna.sudoku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.grading.DifficultyProfile
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.data.game.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Temporary until the New-game sheet (milestone 7), which also remembers the choice.

data class QuickStartUi(
    val spec: GridSpec = GridSpec.Classic,
    val difficulty: Difficulty = Difficulty.Medium,
    val timed: Boolean = false,
    /** A puzzle is being generated. */
    val creating: Boolean = false,
    /** Set once the game exists; the screen navigates to it and calls [HomeViewModel.onGameOpened]. */
    val startedGameId: Long? = null,
) {
    val sizes: List<GridSpec> get() = SIZES
    val difficulties: List<Difficulty> get() = difficultiesFor(spec)

    companion object {
        val SIZES = listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Classic)

        fun difficultiesFor(spec: GridSpec): List<Difficulty> =
            Difficulty.entries.filter { DifficultyProfile.of(spec, it) != null }
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val games: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(QuickStartUi())
    val state: StateFlow<QuickStartUi> = _state.asStateFlow()

    fun selectSize(spec: GridSpec) = _state.update {
        val offered = QuickStartUi.difficultiesFor(spec)
        it.copy(spec = spec, difficulty = if (it.difficulty in offered) it.difficulty else offered.first())
    }

    fun selectDifficulty(difficulty: Difficulty) = _state.update { it.copy(difficulty = difficulty) }

    fun setTimed(timed: Boolean) = _state.update { it.copy(timed = timed) }

    fun start() {
        val current = _state.value
        if (current.creating) return
        _state.update { it.copy(creating = true) }
        viewModelScope.launch {
            val id = games.create(current.spec, current.difficulty, current.timed)
            _state.update { it.copy(creating = false, startedGameId = id) }
        }
    }

    fun onGameOpened() = _state.update { it.copy(startedGameId = null) }
}
