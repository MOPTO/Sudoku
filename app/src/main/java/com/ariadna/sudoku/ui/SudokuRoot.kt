package com.ariadna.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ariadna.sudoku.ui.game.GameScreen
import com.ariadna.sudoku.ui.home.HomeScreen
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
                // Placeholder id until games are persisted (milestone 7).
                HomeScreen(onNewGame = { backStack.add(GameKey(gameId = 0)) })
            }
            entry<GameKey> { key ->
                GameScreen(gameId = key.gameId, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
