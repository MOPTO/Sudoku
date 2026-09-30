package com.ariadna.sudoku.core.grading

import com.ariadna.sudoku.core.logic.Tier
import com.ariadna.sudoku.core.model.GridSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun tierRangesClimbWithDifficulty() {
        for (spec in mathematicalMinimum.keys) {
            val profiles = DifficultyProfile.supported(spec).map { DifficultyProfile.of(spec, it)!! }
            assertTrue(profiles.first().tiers.endInclusive <= Tier.T2, "$spec Easy allows more than singles")
            profiles.zipWithNext { easier, harder ->
                assertTrue(easier.tiers.start <= harder.tiers.start, "$spec tier floors must not drop")
                assertTrue(easier.tiers.endInclusive <= harder.tiers.endInclusive, "$spec tier ceilings must not drop")
            }
        }
    }

    @Test
    fun ceilingOnlyWhereTheSolverCanCheckIt() {
        assertEquals(Tier.T2, DifficultyProfile.of(GridSpec.Classic, Difficulty.Easy)!!.ceiling)
        assertEquals(Tier.T3, DifficultyProfile.of(GridSpec.Classic, Difficulty.Medium)!!.ceiling)
        assertNull(DifficultyProfile.of(GridSpec.Classic, Difficulty.Hard)!!.ceiling)
        assertNull(DifficultyProfile.of(GridSpec.Classic, Difficulty.Expert)!!.ceiling)
        assertNull(DifficultyProfile.of(GridSpec.Six, Difficulty.Hard)!!.ceiling)
        assertNull(DifficultyProfile(20, 30).ceiling)
    }

    @Test
    fun acceptsChecksTierAndScore() {
        val profile = DifficultyProfile(30, 37, Tier.T3..Tier.T3, minScore = 100)
        assertTrue(profile.accepts(Grade(Tier.T3, 100, solved = true)))
        assertFalse(profile.accepts(Grade(Tier.T3, 99, solved = true)))
        assertFalse(profile.accepts(Grade(Tier.T2, 500, solved = true)))
        assertFalse(profile.accepts(Grade(Tier.T4, 500, solved = false)))
        assertTrue(DifficultyProfile(26, 31, Tier.T4..Tier.T7).accepts(Grade(Tier.T4, 300, solved = false)))
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
        assertFailsWith<IllegalArgumentException> { DifficultyProfile(5, 9, Tier.T3..Tier.T2) }
        assertFailsWith<IllegalArgumentException> { DifficultyProfile(5, 9, minScore = -1) }
    }
}
