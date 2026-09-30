package com.ariadna.sudoku.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.ariadna.sudoku.R
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Digit
import com.ariadna.sudoku.ui.game.CellHighlight
import com.ariadna.sudoku.ui.game.CellUi
import com.ariadna.sudoku.ui.game.ValueState
import com.ariadna.sudoku.ui.theme.LocalSudokuColors

/** Semantics keys that expose a cell's highlight and error state to UI tests. */
val CellHighlightKey = SemanticsPropertyKey<CellHighlight>("CellHighlight")
var SemanticsPropertyReceiver.cellHighlight by CellHighlightKey

val CellValueStateKey = SemanticsPropertyKey<ValueState>("CellValueState")
var SemanticsPropertyReceiver.cellValueState by CellValueStateKey

/** Tabular figures keep every digit the same width. */
internal val DigitTextStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun SudokuCell(
    cell: CellUi,
    size: Int,
    boxRows: Int,
    boxCols: Int,
    digitSize: TextUnit,
    noteSize: TextUnit,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSudokuColors.current
    val target = when (cell.highlight) {
        CellHighlight.Selected -> colors.selectedCell
        CellHighlight.Conflict -> colors.errorCell
        CellHighlight.SameDigit -> colors.sameDigitCell
        CellHighlight.Peer -> colors.peerCell
        CellHighlight.None -> colors.boardBackground
    }
    // Default animation spec: MaterialTheme.motionScheme (§7.6) is still internal in Material 3 1.4.0.
    val background by animateColorAsState(target, label = "cellBackground")
    val description = cellDescription(cell, size)
    val selectLabel = stringResource(R.string.cell_select)

    Box(
        modifier = modifier
            .drawBehind { drawRect(background) }
            .clickable { onClick(cell.index) }
            .clearAndSetSemantics {
                contentDescription = description
                testTag = "cell_${cell.index}"
                selected = cell.highlight == CellHighlight.Selected
                cellHighlight = cell.highlight
                cellValueState = cell.valueState
                onClick(label = selectLabel) { onClick(cell.index); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (cell.value != 0) {
            val error = cell.valueState != ValueState.Normal
            Text(
                text = Digit.toChar(cell.value).toString(),
                color = when {
                    error -> colors.errorText
                    cell.isGiven -> colors.givenText
                    else -> colors.userText
                },
                fontSize = digitSize,
                lineHeight = digitSize,
                fontWeight = if (cell.isGiven) FontWeight.SemiBold else FontWeight.Normal,
                // Errors are never color-only (§7.3).
                textDecoration = if (error) TextDecoration.Underline else null,
                style = DigitTextStyle,
            )
        } else if (cell.notes != 0) {
            NotesGrid(
                notes = cell.notes,
                boxRows = boxRows,
                boxCols = boxCols,
                highlightedDigit = cell.notesHighlightedDigit,
                wrong = cell.notesWrong,
                fontSize = noteSize,
            )
        }
    }
}

@Composable
private fun cellDescription(cell: CellUi, size: Int): String {
    val parts = mutableListOf(stringResource(R.string.cell_position, cell.index / size + 1, cell.index % size + 1))
    when {
        cell.value != 0 -> parts += Digit.toChar(cell.value).toString()
        cell.notes != 0 -> parts += stringResource(
            R.string.cell_notes,
            Candidates(cell.notes).toList().joinToString(" ") { Digit.toChar(it).toString() },
        )
        else -> parts += stringResource(R.string.cell_empty)
    }
    if (cell.isGiven) parts += stringResource(R.string.cell_given)
    when (cell.valueState) {
        ValueState.Wrong -> parts += stringResource(R.string.cell_wrong)
        ValueState.Conflict -> parts += stringResource(R.string.cell_conflict)
        ValueState.Normal -> Unit
    }
    return parts.joinToString(", ")
}
