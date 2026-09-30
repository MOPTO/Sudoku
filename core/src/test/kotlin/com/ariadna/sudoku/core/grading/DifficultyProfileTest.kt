package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.model.GridSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DifficultyProfileTest {

    // Smallest possible number of clues for a unique puzzle (§4.6).
    private val mathematicalMinimum = mapOf(GridSpec.Mini4 to 4, GridSpec.Six to 8, GridSpec.Classic to 17)

    @Test
    fun supportedDifficultiesMatchTheLadder() {
        assertEquals(listOf(Difficulty.Easy, Difficulty.Medium), DifficultyProfile.supported(GridSpec.Mini4))
        assertEquals(
            listOf(Difficulty.Easy, Difficulty.Medium, Difficulty.Hard),
            DifficultyProfile.supported(GridSpec.Six),
        )
        assertEquals(Difficulty.entries, DifficultyProfile.supported(GridSpec.Classic))
        assertEquals(emptyList(), DifficultyProfile.supported(GridSpec.Sixteen))
    }

    @Test
    fun everyProfileIsWithinSaneBounds() {
        for ((spec, minimum) in mathematicalMinimum) {
            for (difficulty in DifficultyProfile.supported(spec)) {
                val profile = assertNotNull(DifficultyProfile.of(spec, difficulty))
                assertTrue(profile.minClues >= minimum, "$spec $difficulty below the mathematical minimum")
                assertTrue(profile.maxClues < spec.cellCount, "$spec $difficulty allows a full grid")
            }
        }
    }

    @Test
    fun unofferedPairsHaveNoProfile() {
        assertNull(DifficultyProfile.of(GridSpec.Mini4, Difficulty.Hard))
        assertNull(DifficultyProfile.of(GridSpec.Six, Difficulty.Expert))
        assertNull(DifficultyProfile.of(GridSpec.Eight, Difficulty.Easy))
    }

    @Test
    fun rejectsInvalidRange() {
        assertFailsWith<IllegalArgumentException> { DifficultyProfile(10, 9) }
        assertFailsWith<IllegalArgumentException> { DifficultyProfile(0, 5) }
    }
}
