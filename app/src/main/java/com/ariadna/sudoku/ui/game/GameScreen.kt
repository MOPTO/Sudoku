package com.ariadna.sudoku.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.ui.game.components.CompletionDialog
import com.ariadna.sudoku.ui.game.components.GameToolbar
import com.ariadna.sudoku.ui.game.components.GameTopBar
import com.ariadna.sudoku.ui.game.components.NumberPad
import com.ariadna.sudoku.ui.game.components.PauseOverlay
import com.ariadna.sudoku.ui.game.components.SudokuBoard
import com.ariadna.sudoku.ui.theme.SudokuTheme

/**
 * The game screen, stacked phone layout (§9.2): top bar → board → toolbar → number pad.
 * Stateless: it draws [state] and reports everything through [onAction]. [timerText] is read only by the timer label.
 */
@Composable
fun GameScreen(
    state: GameUiState,
    timerText: () -> String,
    onAction: (GameScreenAction) -> Unit,
    onBack: () -> Unit,
    onOpenGame: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnAction by rememberUpdatedState(onAction)
    LifecycleResumeEffect(Unit) {
        currentOnAction(GameScreenAction.ScreenResumed)
        onPauseOrDispose { currentOnAction(GameScreenAction.ScreenPaused) }
    }

    val ready = state as? GameUiState.Ready
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GameTopBar(
                topBar = ready?.topBar,
                paused = ready?.paused == true,
                timerText = timerText,
                onBack = onBack,
                onPause = { onAction(GameScreenAction.Pause) },
                onResume = { onAction(GameScreenAction.Resume) },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (state) {
            GameUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            GameUiState.NotFound -> LaunchedEffect(Unit) { onBack() }
            is GameUiState.Ready -> {
                StackedGameLayout(state, onAction, contentModifier)
                state.completion?.let { completion ->
                    CompletionDialog(
                        completion = completion,
                        onNextPuzzle = { onAction(GameScreenAction.NextPuzzle) },
                        onHome = onBack,
                        onDismiss = { onAction(GameScreenAction.DismissCompletion) },
                    )
                }
                state.nextGameId?.let { id -> LaunchedEffect(id) { onOpenGame(id) } }
            }
        }
    }
}

@Composable
private fun StackedGameLayout(state: GameUiState.Ready, onAction: (GameScreenAction) -> Unit, modifier: Modifier) {
    val onGameAction: (GameAction) -> Unit = { onAction(GameScreenAction.Game(it)) }
    Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SudokuBoard(
                board = state.board,
                onCellClick = { onGameAction(GameAction.SelectCell(it)) },
                modifier = Modifier.fillMaxSize(),
            )
            // Qualified: inside the Column, the ColumnScope overload would otherwise be picked.
            androidx.compose.animation.AnimatedVisibility(visible = state.paused, modifier = Modifier.matchParentSize()) {
                PauseOverlay(onResume = { onAction(GameScreenAction.Resume) })
            }
        }
        Spacer(Modifier.height(8.dp))
        GameToolbar(state.toolbar, onGameAction)
        Spacer(Modifier.height(8.dp))
        NumberPad(state.pad, onGameAction)
        Spacer(Modifier.height(16.dp))
    }
}

@PreviewLightDark
@Composable
private fun GameScreenPreview() {
    SudokuTheme {
        GameScreen(GamePreviewData.classicInProgress(), timerText = { "4:07" }, onAction = {}, onBack = {}, onOpenGame = {})
    }
}

@PreviewLightDark
@Composable
private fun GameScreenMiniPreview() {
    SudokuTheme {
        GameScreen(GamePreviewData.miniInProgress(), timerText = { "" }, onAction = {}, onBack = {}, onOpenGame = {})
    }
}

@PreviewLightDark
@Composable
private fun GameScreenPausedPreview() {
    SudokuTheme {
        GameScreen(GamePreviewData.classicInProgress(timed = true, paused = true), timerText = { "12:30" }, onAction = {}, onBack = {}, onOpenGame = {})
    }
}
