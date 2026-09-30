package com.ariadna.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ariadna.sudoku.ui.game.GameScreen
import com.ariadna.sudoku.ui.game.GameViewModel
import com.ariadna.sudoku.ui.home.HomeScreen
import com.ariadna.sudoku.ui.home.HomeViewModel
import com.ariadna.sudoku.ui.navigation.GameKey
import com.ariadna.sudoku.ui.navigation.HomeKey

@Composable
fun SudokuRoot() {
    val backStack = rememberNavBackStack(HomeKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                val vm = hiltViewModel<HomeViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                state.startedGameId?.let { id ->
                    LaunchedEffect(id) {
                        vm.onGameOpened()
                        backStack.add(GameKey(id))
                    }
                }
                HomeScreen(
                    state = state,
                    onSelectSize = vm::selectSize,
                    onSelectDifficulty = vm::selectDifficulty,
                    onTimedChange = vm::setTimed,
                    onStart = vm::start,
                )
            }
            entry<GameKey> { key ->
                val vm = hiltViewModel<GameViewModel, GameViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                val timerText = vm.timerText.collectAsStateWithLifecycle()
                GameScreen(
                    state = state,
                    timerText = { timerText.value },
                    onAction = vm::onAction,
                    onBack = { backStack.removeLastOrNull() },
                    // "Next puzzle" replaces this game instead of stacking games (§5.2).
                    onOpenGame = { id -> backStack[backStack.lastIndex] = GameKey(id) },
                )
            }
        },
    )
}
