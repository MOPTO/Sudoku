package com.ariadna.sudoku.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ariadna.sudoku.R
import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.ui.game.PadDigitUi
import com.ariadna.sudoku.ui.game.PadUi

/**
 * One row of digit buttons (§7.4). Cell-first: a tap enters the digit. Digit-first: a tap locks it (again: unlocks).
 */
@Composable
fun NumberPad(pad: PadUi, onAction: (GameAction) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (digit in pad.digits) {
            PadButton(
                digit = digit,
                showRemaining = pad.showRemaining,
                notesMode = pad.notesMode,
                enabled = pad.enabled && !digit.dimmed,
                onClick = {
                    onAction(
                        if (pad.digitFirst) GameAction.LockDigit(if (digit.locked) null else digit.digit)
                        else GameAction.Input(digit.digit),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PadButton(
    digit: PadDigitUi,
    showRemaining: Boolean,
    notesMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = pluralStringResource(R.plurals.pad_digit_remaining, digit.remaining, digit.label.toString(), digit.remaining)
    Surface(
        onClick = onClick,
        enabled = enabled,
        selected = digit.locked,
        shape = MaterialTheme.shapes.medium,
        color = if (digit.locked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .height(64.dp)
            .alpha(if (digit.dimmed) DIMMED_ALPHA else 1f)
            .testTag("pad_${digit.digit}")
            .semantics { if (showRemaining) contentDescription = description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = digit.label.toString(),
                style = if (notesMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                color = if (notesMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
            if (showRemaining) {
                Text(
                    text = digit.remaining.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private const val DIMMED_ALPHA = 0.38f
