package com.example.bag.listener

import com.example.bag.BagPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

class LifecycleListener(
    private val plugin: BagPlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        plugin.scheduleSync(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        plugin.capacityTracker.remove(player.uniqueId)
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                if (player.isOnline) {
                    plugin.capacityService.syncInitial(player)
                }
            },
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        plugin.scheduleSync(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        event.drops.removeIf { plugin.itemFactory.isPlaceholder(it) }
        plugin.capacityService.clearPlaceholders(player)
        plugin.capacityTracker.remove(player.uniqueId)
        plugin.clearPendingSync(player.uniqueId)
        plugin.inventoryGuardListener.cleanup(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        plugin.capacityService.sync(player)
        plugin.capacityService.clearPlaceholders(player)
        plugin.capacityTracker.remove(player.uniqueId)
        plugin.clearPendingSync(player.uniqueId)
        plugin.inventoryGuardListener.cleanup(player.uniqueId)
    }
}
