package com.ariadna.sudoku.ui.game.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.ariadna.sudoku.core.model.Candidates
import com.ariadna.sudoku.core.model.Digit
import com.ariadna.sudoku.ui.theme.LocalSudokuColors

/** Pencil marks as a [boxRows] × [boxCols] mini-grid: digit d sits where it would in a box. */
@Composable
fun NotesGrid(
    notes: Int,
    boxRows: Int,
    boxCols: Int,
    highlightedDigit: Int,
    wrong: Boolean,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSudokuColors.current
    val candidates = Candidates(notes)
    Column(modifier.fillMaxSize()) {
        for (r in 0 until boxRows) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until boxCols) {
                    val digit = r * boxCols + c + 1
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (digit in candidates) {
                            val emphasised = digit == highlightedDigit
                            Text(
                                text = Digit.toChar(digit).toString(),
                                color = when {
                                    wrong -> colors.errorText
                                    emphasised -> colors.userText
                                    else -> colors.noteText
                                },
                                fontSize = fontSize,
                                lineHeight = fontSize,
                                fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
                                style = DigitTextStyle,
                            )
                        }
                    }
                }
            }
        }
    }
}
