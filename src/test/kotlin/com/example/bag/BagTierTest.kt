package com.example.bag

import com.example.bag.bag.BagTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 验证背包等级 ID 与行数的稳定映射。
 *
 * 这些值是持久化协议的一部分：`bag:tier` 和 `config.yml` 都依赖它们。
 */
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
