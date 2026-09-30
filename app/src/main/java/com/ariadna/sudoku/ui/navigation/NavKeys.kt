package com.ariadna.sudoku.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data class GameKey(val gameId: Long) : NavKey
