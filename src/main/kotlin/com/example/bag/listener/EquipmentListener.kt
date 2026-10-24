package com.example.bag.listener

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.example.bag.BagPlugin
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

class EquipmentListener(
    private val plugin: BagPlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityEquipmentChanged(event: EntityEquipmentChangedEvent) {
        val player = event.entity as? Player ?: return
        if (!event.equipmentChanges.containsKey(EquipmentSlot.CHEST)) return
        plugin.scheduleSync(player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        if (event.slot != EquipmentSlot.CHEST) return
        plugin.scheduleSync(event.player)
    }
}
