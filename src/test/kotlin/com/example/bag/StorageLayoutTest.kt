package com.example.bag

import com.example.bag.bag.StorageLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageLayoutTest {
    @Test
    fun `no backpack locks the whole storage area`() {
        assertEquals(9..35, StorageLayout.lockedSlots(0))
        assertTrue(StorageLayout.allowedStorageSlots(0).isEmpty())
        assertEquals((0..8).toList(), StorageLayout.allowedSlots(0))
    }

    @Test
    fun `small backpack unlocks the bottom storage row`() {
        assertEquals(18..35, StorageLayout.lockedSlots(1))
        assertEquals(9..17, StorageLayout.allowedStorageSlots(1))
        assertEquals((0..17).toList(), StorageLayout.allowedSlots(1))
    }

    @Test
    fun `medium backpack unlocks two storage rows`() {
        assertEquals(27..35, StorageLayout.lockedSlots(2))
        assertEquals(9..26, StorageLayout.allowedStorageSlots(2))
    }

    @Test
    fun `large backpack locks nothing`() {
        assertTrue(StorageLayout.lockedSlots(3).isEmpty())
        assertEquals(9..35, StorageLayout.allowedStorageSlots(3))
    }

    @Test
    fun `slot checks respect the current tier`() {
        assertTrue(StorageLayout.isLocked(18, 1))
        assertFalse(StorageLayout.isLocked(17, 1))
        assertFalse(StorageLayout.isLocked(8, 0))
        assertTrue(StorageLayout.isLocked(0, 3).not())
    }

    @Test
    fun `invalid row counts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { StorageLayout.lockedSlots(-1) }
        assertThrows(IllegalArgumentException::class.java) { StorageLayout.lockedSlots(4) }
    }
}
