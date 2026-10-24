package com.example.bag.bag

object StorageLayout {
    const val HOTBAR_START = 0
    const val HOTBAR_END = 8
    const val STORAGE_START = 9
    const val STORAGE_END = 35
    const val MAX_ROWS = 3

    fun lockedSlots(rows: Int): IntRange {
        requireValidRows(rows)
        if (rows >= MAX_ROWS) return IntRange.EMPTY
        return (STORAGE_START + rows * 9)..STORAGE_END
    }

    fun allowedStorageSlots(rows: Int): IntRange {
        requireValidRows(rows)
        if (rows <= 0) return IntRange.EMPTY
        return STORAGE_START..(STORAGE_START + rows * 9 - 1)
    }

    fun allowedSlots(rows: Int): List<Int> {
        requireValidRows(rows)
        return (HOTBAR_START..HOTBAR_END).toList() + allowedStorageSlots(rows).toList()
    }

    fun isLocked(
        slot: Int,
        rows: Int,
    ): Boolean {
        requireValidRows(rows)
        return slot in lockedSlots(rows)
    }

    fun newlyLockedSlots(
        oldRows: Int,
        newRows: Int,
    ): IntRange {
        requireValidRows(oldRows)
        requireValidRows(newRows)
        if (newRows >= oldRows) return IntRange.EMPTY
        return (STORAGE_START + newRows * 9)..(STORAGE_START + oldRows * 9 - 1)
    }

    private fun requireValidRows(rows: Int) {
        require(rows in 0..MAX_ROWS) { "rows must be between 0 and $MAX_ROWS, got $rows" }
    }
}
