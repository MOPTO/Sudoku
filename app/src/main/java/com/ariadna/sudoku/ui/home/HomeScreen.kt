package com.ariadna.sudoku.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ariadna.sudoku.R
import com.ariadna.sudoku.core.grading.Difficulty
import com.ariadna.sudoku.core.model.GridSpec
import com.ariadna.sudoku.ui.components.difficultyLabel
import com.ariadna.sudoku.ui.components.gridSizeLabel
import com.ariadna.sudoku.ui.theme.SudokuTheme

/** Home with a temporary quick-start (size, difficulty, timer). Replaced by the New-game sheet in milestone 7. */
@Composable
fun HomeScreen(
    state: QuickStartUi,
    onSelectSize: (GridSpec) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onTimedChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(32.dp))
            Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_size), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (spec in state.sizes) {
                        FilterChip(
                            selected = spec == state.spec,
                            onClick = { onSelectSize(spec) },
                            label = { Text(gridSizeLabel(spec.size)) },
                        )
                    }
                }
                Text(stringResource(R.string.home_difficulty), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (difficulty in state.difficulties) {
                        FilterChip(
                            selected = difficulty == state.difficulty,
                            onClick = { onSelectDifficulty(difficulty) },
                            label = { Text(difficultyLabel(difficulty)) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_timer),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.timed, onCheckedChange = onTimedChange)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart, enabled = !state.creating) {
                if (state.creating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.home_start))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    SudokuTheme {
        HomeScreen(QuickStartUi(), onSelectSize = {}, onSelectDifficulty = {}, onTimedChange = {}, onStart = {})
    }
}
