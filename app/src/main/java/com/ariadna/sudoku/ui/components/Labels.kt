package com.ariadna.sudoku.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ariadna.sudoku.R
import com.ariadna.sudoku.core.grading.Difficulty

@Composable
fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.Easy -> R.string.difficulty_easy
        Difficulty.Medium -> R.string.difficulty_medium
        Difficulty.Hard -> R.string.difficulty_hard
        Difficulty.Expert -> R.string.difficulty_expert
    },
)

@Composable
fun gridSizeLabel(size: Int): String = stringResource(R.string.grid_size, size)
