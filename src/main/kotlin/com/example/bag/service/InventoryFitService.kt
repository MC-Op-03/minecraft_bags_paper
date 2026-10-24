package com.example.bag.service

import com.example.bag.bag.StorageLayout
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

class InventoryFitService {
    fun wouldFit(
        inventory: PlayerInventory,
        item: ItemStack,
        rows: Int,
    ): Boolean {
        if (item.type.isAir || item.amount <= 0) return true

        var remaining = item.amount
        val slots = StorageLayout.allowedSlots(rows)

        for (slot in slots) {
            val existing = inventory.getItem(slot) ?: continue
            if (existing.type.isAir || !existing.isSimilar(item)) continue
            remaining -= minOf(existing.maxStackSize - existing.amount, remaining)
            if (remaining <= 0) return true
        }

        for (slot in slots) {
            val existing = inventory.getItem(slot)
            if (existing == null || existing.type.isAir) {
                remaining -= minOf(item.maxStackSize, remaining)
                if (remaining <= 0) return true
            }
        }
        return false
    }

    fun addToAllowed(
        inventory: PlayerInventory,
        item: ItemStack,
        rows: Int,
    ): ItemStack? {
        if (item.type.isAir || item.amount <= 0) return null

        val remaining = item.clone()
        val slots = StorageLayout.allowedSlots(rows)

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
