package com.ariadna.sudoku.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ariadna.sudoku.R
import com.ariadna.sudoku.ui.game.CompletionUi
import com.ariadna.sudoku.util.formatElapsed

/** A plain M3 dialog: time (if timed), mistakes (if counted) and hints used. No scores or celebration (§7.5). */
@Composable
fun CompletionDialog(
    completion: CompletionUi,
    onNextPuzzle: () -> Unit,
    onHome: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.completion_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                completion.elapsedMs?.let { Text(stringResource(R.string.completion_time, formatElapsed(it))) }
                completion.mistakes?.let { Text(stringResource(R.string.completion_mistakes, it)) }
                Text(stringResource(R.string.completion_hints, completion.hintsUsed))
            }
        },
        confirmButton = {
            TextButton(onClick = onNextPuzzle) { Text(stringResource(R.string.completion_next)) }
        },
        dismissButton = {
            TextButton(onClick = onHome, modifier = Modifier.padding(end = 4.dp)) {
                Text(stringResource(R.string.completion_home))
            }
        },
    )
}
