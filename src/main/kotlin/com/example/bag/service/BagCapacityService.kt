package com.example.bag.service

import com.example.bag.BagPlugin
import com.example.bag.bag.BagItemFactory
import com.example.bag.bag.BagItemResolver
import com.example.bag.bag.StorageLayout
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

class BagCapacityService(
    private val plugin: BagPlugin,
    private val itemFactory: BagItemFactory,
    private val itemResolver: BagItemResolver,
    private val tracker: CapacityTracker,
    private val inventoryFit: InventoryFitService,
) {
    fun rowsOf(player: Player): Int {
        val chestplate = player.inventory.chestplate
        return itemResolver.tierOf(chestplate)?.rows ?: 0
    }

    fun syncInitial(player: Player) {
        if (!player.isOnline) return
        tracker.set(player.uniqueId, rowsOf(player))
        sync(player)
    }

    fun sync(player: Player) {
        if (!player.isOnline) return

        val newRows = rowsOf(player)
        val previousRows = tracker.get(player.uniqueId)
        if (previousRows != null && newRows < previousRows) {
            val dropped = dropSlots(player, StorageLayout.newlyLockedSlots(previousRows, newRows))
            if (dropped > 0) {
                player.sendActionBar(plugin.bagConfig.messages.capacityShrunk)
            }
        }

        healLockedSlots(player, newRows)
        applyPlaceholders(player, newRows)
        tracker.set(player.uniqueId, newRows)
    }

    fun clearPlaceholders(player: Player) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (itemFactory.isPlaceholder(inventory.getItem(slot))) {
                inventory.setItem(slot, null)
            }
        }
    }

    fun give(
        player: Player,
        item: ItemStack,
    ): Boolean {
        sync(player)
        val leftover = inventoryFit.addToAllowed(player.inventory, item, rowsOf(player))
        if (leftover == null) return true

        if (!dropItem(player, leftover)) {
            plugin.logger.warning("Failed to drop ${leftover.type} x${leftover.amount} for ${player.name}")
        }
        return false
    }

    private fun dropSlots(
        player: Player,
        slots: IntRange,
    ): Int {
        if (slots.isEmpty()) return 0

        val inventory = player.inventory
        var dropped = 0
        for (slot in slots) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type.isAir) continue

            inventory.setItem(slot, null)
            if (itemFactory.isPlaceholder(item)) continue
            if (dropItem(player, item)) {
                dropped++
            } else {
                inventory.setItem(slot, item)
            }
        }
        return dropped
    }

    private fun healLockedSlots(
        player: Player,
        rows: Int,
    ) {
        val lockedSlots = StorageLayout.lockedSlots(rows)
        if (lockedSlots.isEmpty()) return

        val inventory = player.inventory
        for (slot in lockedSlots) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type.isAir || itemFactory.isPlaceholder(item)) continue

            inventory.setItem(slot, null)
            val leftover = inventoryFit.addToAllowed(inventory, item, rows)
            if (leftover == null) continue

            if (dropItem(player, leftover)) {
                plugin.logger.info(
                    "Moved item out of locked slot $slot for ${player.name}; could not fit and dropped " +
                        "${leftover.type} x${leftover.amount}",
                )
            } else {
                inventory.setItem(slot, leftover)
                plugin.logger.warning(
                    "Could not move or drop item from locked slot $slot for ${player.name}",
                )
            }
        }
    }

    private fun applyPlaceholders(
        player: Player,
        rows: Int,
    ) {
        val inventory = player.inventory
        val lockedSlots = StorageLayout.lockedSlots(rows)

        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (itemFactory.isPlaceholder(item) && slot !in lockedSlots) {
                inventory.setItem(slot, null)
            }
        }

        for (slot in lockedSlots) {
            val item = inventory.getItem(slot)
            if (item != null && itemFactory.isPlaceholder(item)) continue
            if (item != null && !item.type.isAir) {
                plugin.logger.warning(
                    "Locked slot $slot for ${player.name} still contains ${item.type}; retrying on next sync",
                )
                continue
            }
            inventory.setItem(slot, itemFactory.createPlaceholder())
        }
    }

    private fun dropItem(
        player: Player,
        item: ItemStack,
    ): Boolean =
        try {
            player.world.dropItemNaturally(player.location, item)
            true
        } catch (exception: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to drop ${item.type} for ${player.name}", exception)
            false
        }
}
