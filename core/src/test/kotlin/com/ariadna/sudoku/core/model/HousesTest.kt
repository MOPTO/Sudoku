package com.ariadna.sudoku.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HousesTest {

    private val specs = listOf(GridSpec.Mini4, GridSpec.Six, GridSpec.Eight, GridSpec.Classic, GridSpec.Twelve, GridSpec.Sixteen)

    @Test
    fun houseAndPeerCounts() {
        assertCounts(GridSpec.Classic, houses = 27, peers = 20)
        assertCounts(GridSpec.Six, houses = 18, peers = 12)
        assertCounts(GridSpec.Mini4, houses = 12, peers = 7)
    }

    private fun assertCounts(spec: GridSpec, houses: Int, peers: Int) {
        val h = Houses.of(spec)
        assertEquals(houses, h.all.size, "houses for $spec")
        h.peers.forEach { assertEquals(peers, it.size, "peers for $spec") }
    }

    @Test
    fun everyHouseHasSizeDistinctCells() {
        for (spec in specs) {
            Houses.of(spec).all.forEach { house ->
                assertEquals(spec.size, house.size)
                assertEquals(spec.size, house.toSet().size)
                house.forEach { assertTrue(it in 0 until spec.cellCount) }
            }
        }
    }

    @Test
    fun everyCellIsInExactlyThreeHouses() {
        for (spec in specs) {
            val h = Houses.of(spec)
            h.housesOf.forEachIndexed { cell, hs ->
                assertEquals(3, hs.size)
                hs.forEach { assertTrue(cell in h.all[it]) }
            }
        }
    }

    @Test
    fun boxesMatchCellIndexHelper() {
        for (spec in specs) {
            val boxes = Houses.of(spec).all.subList(2 * spec.size, 3 * spec.size)
            boxes.forEachIndexed { b, cells -> cells.forEach { assertEquals(b, spec.box(it)) } }
        }
    }

    @Test
    fun peerRelationIsSymmetricAndExcludesSelf() {
        for (spec in specs) {
            val peers = Houses.of(spec).peers
            peers.forEachIndexed { cell, ps ->
                assertTrue(cell !in ps)
                ps.forEach { assertTrue(cell in peers[it]) }
            }
        }
    }

    @Test
    fun housesAreCachedPerSpec() {
        assertSame(Houses.of(GridSpec.Classic), Houses.of(GridSpec(9, 3, 3)))
    }

    @Test
    fun invalidSpecIsRejected() {
        assertFailsWith<IllegalArgumentException> { GridSpec(9, 2, 4) }
        assertFailsWith<IllegalArgumentException> { GridSpec(25, 5, 5) }
    }
}
