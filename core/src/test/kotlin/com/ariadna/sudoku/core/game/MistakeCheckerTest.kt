package com.ariadna.sudoku.core.game

import com.ariadna.sudoku.core.game.GameFixtures.enter
import com.ariadna.sudoku.core.game.GameFixtures.note
import kotlin.test.Test
import kotlin.test.assertEquals

class MistakeCheckerTest {

    private val start = GameFixtures.classic()

    @Test
    fun conflictsReportBothDuplicates() {
        assertEquals(emptySet(), MistakeChecker.conflicts(start.session.spec, start.session.values))
        val duplicate = start.enter(2, 5).session                    // 5 is given in cell 0
        assertEquals(setOf(0, 2), MistakeChecker.conflicts(duplicate.spec, duplicate.values))
        val wrongButFine = start.enter(2, 1).session                 // wrong, but no duplicate
        assertEquals(emptySet(), MistakeChecker.conflicts(wrongButFine.spec, wrongButFine.values))
    }

    @Test
    fun wrongCellsCompareUserDigitsWithTheSolution() {
        assertEquals(emptySet(), MistakeChecker.wrongCells(start.session))
        assertEquals(setOf(2), MistakeChecker.wrongCells(start.enter(2, 1).enter(3, 6).session))
    }

    @Test
    fun wrongNoteCellsAreCellsWhoseNotesMissTheSolutionDigit() {
        val state = start.note(2, 1, 2).note(3, 4).note(10, 7, 1)   // solutions: 2 → 4, 3 → 6, 10 → 7
        assertEquals(setOf(2, 3), MistakeChecker.wrongNoteCells(state.session))
        assertEquals(emptySet(), MistakeChecker.wrongNoteCells(start.session), "cells without notes are ignored")
    }
}
