package com.ariadna.sudoku.ui.game.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ariadna.sudoku.R
import com.ariadna.sudoku.core.game.GameAction
import com.ariadna.sudoku.ui.game.ToolbarUi

/** Undo, Redo, Erase, Notes toggle, Auto-notes and (only in MistakeMode.Off) Check (§7.4). */
@Composable
fun GameToolbar(toolbar: ToolbarUi, onAction: (GameAction) -> Unit, modifier: Modifier = Modifier) {
    val enabled = toolbar.enabled
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToolButton(R.drawable.ic_undo, R.string.tool_undo, enabled && toolbar.canUndo) { onAction(GameAction.Undo) }
        ToolButton(R.drawable.ic_redo, R.string.tool_redo, enabled && toolbar.canRedo) { onAction(GameAction.Redo) }
        ToolButton(R.drawable.ic_backspace, R.string.tool_erase, enabled) { onAction(GameAction.Erase) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconToggleButton(
                checked = toolbar.notesMode,
                onCheckedChange = { onAction(GameAction.ToggleNotesMode) },
                enabled = enabled,
            ) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.tool_notes))
            }
            ToolLabel(R.string.tool_notes)
        }
        ToolButton(R.drawable.ic_auto_notes, R.string.tool_auto_notes, enabled) { onAction(GameAction.FillAllNotes) }
        AnimatedVisibility(visible = toolbar.showCheck) {
            ToolButton(R.drawable.ic_done_all, R.string.tool_check, enabled) { onAction(GameAction.CheckBoard) }
        }
    }
}

@Composable
private fun ToolButton(@DrawableRes icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(painterResource(icon), contentDescription = stringResource(label))
        }
        ToolLabel(label)
    }
}

@Composable
private fun ToolLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics {},   // the button already announces it
    )
}
