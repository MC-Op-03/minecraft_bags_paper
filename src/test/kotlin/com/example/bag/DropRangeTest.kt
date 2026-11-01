package com.example.bag

import com.example.bag.bag.StorageLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证降级/卸下时受影响槽位的边界。
 *
 * 这些范围直接决定哪些物品会被丢弃，必须和 StorageLayout 的解锁公式成对维护。
 */
class DropRangeTest {
    @Test
    fun `removing a large backpack drops all storage rows`() {
        assertEquals(9..35, StorageLayout.newlyLockedSlots(3, 0))
    }

    @Test
    fun `downgrading from large to small drops the bottom two rows`() {
        assertEquals(9..26, StorageLayout.newlyLockedSlots(3, 1))
    }

    @Test
    fun `downgrading from large to medium drops the bottom row`() {
        assertEquals(9..17, StorageLayout.newlyLockedSlots(3, 2))
    }

    @Test
    fun `downgrading from small to none drops the top row`() {
        assertEquals(27..35, StorageLayout.newlyLockedSlots(1, 0))
    }

    @Test
    fun `same or larger capacity drops nothing`() {
        assertTrue(StorageLayout.newlyLockedSlots(1, 1).isEmpty())
        assertTrue(StorageLayout.newlyLockedSlots(1, 2).isEmpty())
        assertTrue(StorageLayout.newlyLockedSlots(0, 3).isEmpty())
    }
}
