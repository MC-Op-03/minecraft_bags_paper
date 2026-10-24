package com.example.bag

import com.example.bag.bag.BagTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BagTierTest {
    @Test
    fun `tiers map to the expected storage rows`() {
        assertEquals(1, BagTier.SMALL.rows)
        assertEquals(2, BagTier.MEDIUM.rows)
        assertEquals(3, BagTier.LARGE.rows)
    }

    @Test
    fun `tier ids round trip`() {
        for (tier in BagTier.entries) {
            assertEquals(tier, BagTier.fromId(tier.id))
        }
    }

    @Test
    fun `unknown tier id is ignored`() {
        assertNull(BagTier.fromId(null))
        assertNull(BagTier.fromId(""))
        assertNull(BagTier.fromId("huge"))
    }
}
