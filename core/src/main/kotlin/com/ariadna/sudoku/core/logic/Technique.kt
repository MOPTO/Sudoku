package com.ariadna.sudoku.core.logic

import kotlinx.serialization.Serializable

/** Technique tiers (§4.5), easiest first. [T7] (Extreme) means no technique works: trial & error. */
@Serializable
enum class Tier { T1, T2, T3, T4, T5, T6, T7 }

/**
 * Human solving techniques in the order the [LogicalSolver] tries them (easiest first).
 * [baseCost] is added to the grade's score for every step that uses the technique.
 */
enum class Technique(val tier: Tier, val baseCost: Int) {
    FullHouse(Tier.T1, 4),
    NakedSingle(Tier.T1, 4),
    HiddenSingle(Tier.T2, 14),
    LockedPointing(Tier.T3, 50),
    LockedClaiming(Tier.T3, 50),
    NakedPair(Tier.T3, 60),
    HiddenPair(Tier.T3, 70),
}
