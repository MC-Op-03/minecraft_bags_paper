package com.example.bag

import com.example.bag.bag.StorageLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DropRangeTest {
    @Test
    fun `removing a large backpack drops all storage rows`() {
        assertEquals(9..35, StorageLayout.newlyLockedSlots(3, 0))
    }

    @Test
    fun `downgrading from large to small drops the upper two rows`() {
        assertEquals(18..35, StorageLayout.newlyLockedSlots(3, 1))
    }

    @Test
    fun `downgrading from large to medium drops the top row`() {
        assertEquals(27..35, StorageLayout.newlyLockedSlots(3, 2))
    }

    @Test
    fun `same or larger capacity drops nothing`() {
        assertTrue(StorageLayout.newlyLockedSlots(1, 1).isEmpty())
        assertTrue(StorageLayout.newlyLockedSlots(1, 2).isEmpty())
        assertTrue(StorageLayout.newlyLockedSlots(0, 3).isEmpty())
    }
}
