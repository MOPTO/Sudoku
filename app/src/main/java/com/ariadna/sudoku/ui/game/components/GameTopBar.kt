package com.ariadna.sudoku.ui.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import com.ariadna.sudoku.R
import com.ariadna.sudoku.ui.components.difficultyLabel
import com.ariadna.sudoku.ui.components.gridSizeLabel
import com.ariadna.sudoku.ui.game.TopBarUi

/**
 * Back, size + difficulty, the mistakes counter (Immediate mode only) and, for timed games only,
 * the timer and a Pause/Resume button (§7.4). [timerText] is read only by the timer label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    topBar: TopBarUi?,
    paused: Boolean,
    timerText: () -> String,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (topBar != null) {
                Text(stringResource(R.string.game_title, gridSizeLabel(topBar.size), difficultyLabel(topBar.difficulty)))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            if (topBar == null) return@TopAppBar
            topBar.mistakes?.let { mistakes ->
                Text(
                    text = stringResource(R.string.game_mistakes, mistakes),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            if (topBar.timed) {
                TimerLabel(timerText, enabled = !paused, onPause = onPause)
                IconButton(onClick = if (paused) onResume else onPause) {
                    Icon(
                        painterResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause),
                        contentDescription = stringResource(if (paused) R.string.game_resume else R.string.game_pause),
                    )
                }
            }
        },
    )
}

/** The only composable that reads the timer, so the once-per-second update recomposes just this label. */
@Composable
private fun TimerLabel(timerText: () -> String, enabled: Boolean, onPause: () -> Unit) {
    val text = timerText()
    val description = stringResource(R.string.game_timer_description, text)
    val pauseLabel = stringResource(R.string.game_pause)
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.merge(DigitTextStyle),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onPause)
            .clearAndSetSemantics {
                contentDescription = description
                if (enabled) onClick(label = pauseLabel) { onPause(); true }
            }
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}
