package com.example.bag.listener

import com.example.bag.BagPlugin
import com.example.bag.bag.StorageLayout
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import java.util.UUID

class InventoryGuardListener(
    private val plugin: BagPlugin,
) : Listener {
    private val lastNoticeAt = HashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        if (isPlaceholder(event.currentItem) || isPlaceholder(event.cursor)) {
            event.isCancelled = true
            return
        }

        val rows = plugin.capacityService.rowsOf(player)
        val clickedPlayerInventory = event.clickedInventory === player.inventory

        if (clickedPlayerInventory) {
            if (StorageLayout.isLocked(event.slot, rows) || isPlaceholder(event.currentItem)) {
                event.isCancelled = true
                notifyLocked(player)
                return
            }
        } else if (event.isShiftClick) {
            val item = event.currentItem
            if (item != null && !item.type.isAir && !plugin.inventoryFit.wouldFit(player.inventory, item, rows)) {
                event.isCancelled = true
                notifyNoSpace(player)
                return
            }
        }

        if (clickedPlayerInventory || event.isShiftClick) {
            plugin.scheduleSync(player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return

        if (isPlaceholder(event.oldCursor) || isPlaceholder(event.cursor)) {
            event.isCancelled = true
            return
        }

        val rows = plugin.capacityService.rowsOf(player)
        for (rawSlot in event.rawSlots) {
            if (isPlaceholder(event.view.getItem(rawSlot))) {
                event.isCancelled = true
                return
            }
            if (event.view.getInventory(rawSlot) !== player.inventory) continue
            if (StorageLayout.isLocked(event.view.convertSlot(rawSlot), rows)) {
                event.isCancelled = true
                notifyLocked(player)
                return
            }
        }

        plugin.scheduleSync(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPickup(event: PlayerAttemptPickupItemEvent) {
        val player = event.player
        val item = event.item.itemStack
        if (isPlaceholder(item)) {
            event.isCancelled = true
            return
        }

        val rows = plugin.capacityService.rowsOf(player)
        if (!plugin.inventoryFit.wouldFit(player.inventory, item, rows)) {
            event.isCancelled = true
            return
        }

        plugin.scheduleSync(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        if (isPlaceholder(event.itemDrop.itemStack)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (isPlaceholder(event.entity.itemStack)) {
            event.isCancelled = true
        }
    }

    fun cleanup(playerId: UUID) {
        lastNoticeAt.remove(playerId)
    }

    private fun notifyLocked(player: Player) {
        sendNotice(player, plugin.bagConfig.messages.lockedSlot)
    }

    private fun notifyNoSpace(player: Player) {
        sendNotice(player, plugin.bagConfig.messages.noSpace)
    }

    private fun sendNotice(
        player: Player,
        message: net.kyori.adventure.text.Component,
    ) {
        val now = System.currentTimeMillis()
        val last = lastNoticeAt[player.uniqueId]
        if (last != null && now - last < NOTICE_INTERVAL_MS) return
        lastNoticeAt[player.uniqueId] = now
        player.sendActionBar(message)
    }

    private fun isPlaceholder(item: org.bukkit.inventory.ItemStack?): Boolean = plugin.itemFactory.isPlaceholder(item)

    private companion object {
        const val NOTICE_INTERVAL_MS = 1_000L
    }
}
