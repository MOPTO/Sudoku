package com.ariadna.sudoku.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Board-specific color tokens (§8.1). Board composables read only these, never the ColorScheme directly. */
@Immutable
data class SudokuColors(
    val boardBackground: Color,
    val gridLineThin: Color,
    val gridLineThick: Color,
    val givenText: Color,
    val userText: Color,
    val noteText: Color,
    val selectedCell: Color,
    val peerCell: Color,
    val sameDigitCell: Color,
    val errorText: Color,
    val errorCell: Color,
    val hintCell: Color,
    /** Diagonal / windoku tint. */
    val variantRegion: Color,
)

val LocalSudokuColors = staticCompositionLocalOf<SudokuColors> { error("No SudokuColors") }

/** Default derivation: any ColorScheme (incl. dynamic) gives good board colors. */
fun ColorScheme.toSudokuColors() = SudokuColors(
    boardBackground = surface,
    gridLineThin = outlineVariant,
    gridLineThick = onSurface,
    givenText = onSurface,
    userText = primary,
    noteText = onSurfaceVariant,
    selectedCell = primaryContainer,
    peerCell = surfaceContainerHighest,
    sameDigitCell = secondaryContainer,
    errorText = error,
    errorCell = errorContainer,
    hintCell = tertiaryContainer,
    variantRegion = surfaceContainerHigh,
)
