package com.example.bag.bag

/**
 * 原版玩家物品栏的槽位布局与背包容量规则。
 *
 * 原版玩家物品栏由三部分组成：
 * - 快捷栏 `0..8`：始终可用；
 * - 存储区 `9..35`：共 3 行，受背包等级限制；
 * - 盔甲、副手等槽位不在这里处理。
 *
 * 当前规则是“从上往下解锁”：
 * - 小背包：只解锁最上面一行 `27..35`；
 * - 中背包：解锁 `18..35`；
 * - 大背包：解锁全部 `9..35`。
 *
 * 这里的范围都是闭区间（`IntRange`）。没有槽位时返回 [IntRange.EMPTY]，
 * 调用方应使用 `isEmpty()` 或 `in` 判断，不要假设范围一定非空。
 */
object StorageLayout {
    /** 快捷栏起始槽位。 */
    const val HOTBAR_START = 0

    /** 快捷栏结束槽位。 */
    const val HOTBAR_END = 8

    /** 存储区起始槽位。 */
    const val STORAGE_START = 9

    /** 存储区结束槽位。 */
    const val STORAGE_END = 35

    /** 原版玩家物品栏的存储行总数。 */
    const val MAX_ROWS = 3

    /**
     * 返回当前等级锁定的存储槽位。
     *
     * 大背包没有锁定槽，返回 [IntRange.EMPTY]。
     */
    fun lockedSlots(rows: Int): IntRange {
        requireValidRows(rows)
        if (rows >= MAX_ROWS) return IntRange.EMPTY
        // 从上往下解锁：未解锁的行保持在存储区的上半部分。
        return STORAGE_START..(STORAGE_START + (MAX_ROWS - rows) * 9 - 1)
    }

    /**
     * 返回当前等级可用的存储槽位。
     *
     * 无背包时返回 [IntRange.EMPTY]；快捷栏不包含在这个范围内。
     */
    fun allowedStorageSlots(rows: Int): IntRange {
        requireValidRows(rows)
        if (rows <= 0) return IntRange.EMPTY
        // rows=1 -> 27..35, rows=2 -> 18..35, rows=3 -> 9..35
        return (STORAGE_START + (MAX_ROWS - rows) * 9)..STORAGE_END
    }

    /**
     * 返回快捷栏与可用存储区的并集，供“能否放入玩家物品栏”的模拟使用。
     */
    fun allowedSlots(rows: Int): List<Int> {
        requireValidRows(rows)
        return (HOTBAR_START..HOTBAR_END).toList() + allowedStorageSlots(rows).toList()
    }

    /**
     * 判断单个槽位在当前等级下是否被锁定。
     *
     * 快捷栏、盔甲栏、副手栏不在存储区范围内，因此永远返回 `false`。
     */
    fun isLocked(
        slot: Int,
        rows: Int,
    ): Boolean {
        requireValidRows(rows)
        return slot in lockedSlots(rows)
    }

    /**
     * 计算从 [oldRows] 降级到 [newRows] 时“新变成锁定”的槽位。
     *
     * 这个方法只负责给出需要丢弃的范围；实际清空槽位、掉落物品
     * 由 [com.example.bag.service.BagCapacityService] 完成。
     *
     * 示例：
     * - 3 -> 1：9..26
     * - 3 -> 2：9..17
     * - 1 -> 0：27..35
     */
    fun newlyLockedSlots(
        oldRows: Int,
        newRows: Int,
    ): IntRange {
        requireValidRows(oldRows)
        requireValidRows(newRows)
        if (newRows >= oldRows) return IntRange.EMPTY
        // 降级后，从 newRows 对应的顶部边界开始，到 oldRows 对应的边界结束。
        return (STORAGE_START + (MAX_ROWS - oldRows) * 9)..(STORAGE_START + (MAX_ROWS - newRows) * 9 - 1)
    }

    /**
     * 所有对外方法都要求 `rows` 在 `0..MAX_ROWS`，防止配置/代码错误
     * 生成难以排查的负范围或越界范围。
     */
    private fun requireValidRows(rows: Int) {
        require(rows in 0..MAX_ROWS) { "rows must be between 0 and $MAX_ROWS, got $rows" }
    }
}
