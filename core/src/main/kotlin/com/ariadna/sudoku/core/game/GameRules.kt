package com.ariadna.sudoku.core.game

/** How mistakes are shown (§7.3). Only [Immediate] counts mistakes; only [Off] offers a manual Check. */
enum class MistakeMode { Off, Conflicts, Immediate }

/** The settings the reducer depends on. */
data class GameRules(
    val mistakeMode: MistakeMode = MistakeMode.Immediate,
    val autoRemoveNotes: Boolean = true,
)
