package com.example.bag.service

import com.example.bag.bag.StorageLayout
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * 只针对允许槽位的物品放置模拟与写入。
 *
 * 用途：
 * - 在 `InventoryClickEvent` / `PlayerAttemptPickupItemEvent` 中提前判断
 *   “原版会不会把物品放入锁定槽”；
 * - 在 `/bag give` 等主动发放场景中，只写入快捷栏和已解锁存储区。
 *
 * 这里不读取/修改占位物品；调用方必须保证锁定槽有占位或另有拦截。
 * 所有方法在主线程调用，参数 [inventory] 必须是玩家自己的物品栏。
 */
class InventoryFitService {
    /**
     * 判断 [item] 是否能完整放入 [rows] 对应的允许槽位。
     *
     * 先尝试与已有同类物品堆叠，再使用空槽；不会修改物品栏。
     */
    fun wouldFit(
        inventory: PlayerInventory,
        item: ItemStack,
        rows: Int,
    ): Boolean {
        if (item.type.isAir || item.amount <= 0) return true

        var remaining = item.amount
        val slots = StorageLayout.allowedSlots(rows)

        // 第一遍：只考虑已有同类物品的剩余堆叠空间。
        for (slot in slots) {
            val existing = inventory.getItem(slot) ?: continue
            if (existing.type.isAir || !existing.isSimilar(item)) continue
            remaining -= minOf(existing.maxStackSize - existing.amount, remaining)
            if (remaining <= 0) return true
        }

        // 第二遍：只考虑空槽。`maxStackSize` 可能来自物品组件，不能写死 64。
        for (slot in slots) {
            val existing = inventory.getItem(slot)
            if (existing == null || existing.type.isAir) {
                remaining -= minOf(item.maxStackSize, remaining)
                if (remaining <= 0) return true
            }
        }
        return false
    }

    /**
     * 把 [item] 放入允许槽位。
     *
     * @return 未能放入的剩余物品；全部放入时返回 `null`
     */
    fun addToAllowed(
        inventory: PlayerInventory,
        item: ItemStack,
        rows: Int,
    ): ItemStack? {
        if (item.type.isAir || item.amount <= 0) return null

        val remaining = item.clone()
        val slots = StorageLayout.allowedSlots(rows)

        // 先堆叠到已有同类物品。
        for (slot in slots) {
            if (remaining.amount <= 0) break
            val existing = inventory.getItem(slot) ?: continue
            if (existing.type.isAir || !existing.isSimilar(remaining)) continue
            val space = existing.maxStackSize - existing.amount
            if (space <= 0) continue
            val moved = minOf(space, remaining.amount)
            existing.amount += moved
            inventory.setItem(slot, existing)
            remaining.amount -= moved
        }

        // 再填入空槽；按物品自身的最大堆叠数切分。
        for (slot in slots) {
            if (remaining.amount <= 0) break
            val existing = inventory.getItem(slot)
            if (existing == null || existing.type.isAir) {
                val moved = minOf(remaining.maxStackSize, remaining.amount)
                val placed = remaining.clone()
                placed.amount = moved
                inventory.setItem(slot, placed)
                remaining.amount -= moved
            }
        }

        return remaining.takeIf { it.amount > 0 }
    }
}
