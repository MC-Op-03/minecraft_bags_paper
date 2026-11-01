package com.example.bag

import com.example.bag.bag.StorageLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证从上往下解锁的槽位边界。
 *
 * 槽位编号是 Bukkit PlayerInventory 的存储索引，不是界面 raw slot：
 * 0–8 快捷栏，9–35 存储区。修改解锁方向时必须重写这些断言。
 */
class StorageLayoutTest {
    @Test
    fun `no backpack locks the whole storage area`() {
        assertEquals(9..35, StorageLayout.lockedSlots(0))
        assertTrue(StorageLayout.allowedStorageSlots(0).isEmpty())
        assertEquals((0..8).toList(), StorageLayout.allowedSlots(0))
    }

    @Test
    fun `small backpack unlocks the top storage row`() {
        assertEquals(9..26, StorageLayout.lockedSlots(1))
        assertEquals(27..35, StorageLayout.allowedStorageSlots(1))
        assertEquals((0..8).toList() + (27..35).toList(), StorageLayout.allowedSlots(1))
    }

    @Test
    fun `medium backpack unlocks the top two storage rows`() {
        assertEquals(9..17, StorageLayout.lockedSlots(2))
        assertEquals(18..35, StorageLayout.allowedStorageSlots(2))
    }

    @Test
    fun `large backpack locks nothing`() {
        assertTrue(StorageLayout.lockedSlots(3).isEmpty())
        assertEquals(9..35, StorageLayout.allowedStorageSlots(3))
    }

    @Test
    fun `slot checks respect the current tier`() {
        assertTrue(StorageLayout.isLocked(26, 1))
        assertFalse(StorageLayout.isLocked(27, 1))
        assertTrue(StorageLayout.isLocked(17, 2))
        assertFalse(StorageLayout.isLocked(18, 2))
        assertFalse(StorageLayout.isLocked(8, 0))
        assertTrue(StorageLayout.isLocked(9, 0))
        assertFalse(StorageLayout.isLocked(0, 3))
    }

    @Test
    fun `invalid row counts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { StorageLayout.lockedSlots(-1) }
        assertThrows(IllegalArgumentException::class.java) { StorageLayout.lockedSlots(4) }
    }
}
