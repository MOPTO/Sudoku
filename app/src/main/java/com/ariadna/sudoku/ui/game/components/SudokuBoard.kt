package com.ariadna.sudoku.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.ariadna.sudoku.ui.game.BoardUi
import com.ariadna.sudoku.ui.theme.LocalSudokuColors

/**
 * The square board: N×N [SudokuCell]s, as large as fits (§7.1). Grid lines are drawn once over the cells:
 * thin between cells, thick on box borders and around the board.
 */
@Composable
fun SudokuBoard(board: BoardUi, onCellClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSudokuColors.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        val n = board.size
        val density = LocalDensity.current
        val digitSize = with(density) { (side / n * 0.6f).toSp() }
        val noteSize = with(density) { (side / n / board.boxCols * 0.8f).toSp() }
        Layout(
            content = {
                for (cell in board.cells) {
                    key(cell.index) {
                        SudokuCell(
                            cell = cell,
                            size = n,
                            boxRows = board.boxRows,
                            boxCols = board.boxCols,
                            digitSize = digitSize,
                            noteSize = noteSize,
                            onClick = onCellClick,
                        )
                    }
                }
            },
            modifier = Modifier
                .size(side)
                .background(colors.boardBackground)
                .drawWithContent {
                    drawContent()
                    val thin = 1.dp.toPx()
                    val thick = 2.dp.toPx()
                    val w = size.width
                    for (i in 0..n) {
                        val p = edge(i, n, w.toInt()).toFloat().coerceIn(thick / 2, w - thick / 2)
                        val vThick = i % board.boxCols == 0
                        val hThick = i % board.boxRows == 0
                        drawLine(
                            if (vThick) colors.gridLineThick else colors.gridLineThin,
                            Offset(p, 0f), Offset(p, w), if (vThick) thick else thin,
                        )
                        drawLine(
                            if (hThick) colors.gridLineThick else colors.gridLineThin,
                            Offset(0f, p), Offset(w, p), if (hThick) thick else thin,
                        )
                    }
                },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val placeables = measurables.mapIndexed { i, m ->
                val cellW = edge(i % n + 1, n, width) - edge(i % n, n, width)
                val cellH = edge(i / n + 1, n, width) - edge(i / n, n, width)
                m.measure(Constraints.fixed(cellW, cellH))
            }
            layout(width, width) {
                placeables.forEachIndexed { i, p -> p.place(edge(i % n, n, width), edge(i / n, n, width)) }
            }
        }
    }
}

/** Pixel position of the [i]-th cell edge, spreading the rounding remainder evenly over the cells. */
private fun edge(i: Int, n: Int, width: Int): Int = i * width / n
