package com.example.bag.listener

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.example.bag.BagPlugin
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

/**
 * 监听胸甲栏变化并触发一次延迟同步。
 *
 * 同时监听两个事件：
 * - [EntityEquipmentChangedEvent] 覆盖 CE 自定义物品和其他 equippable 物品；
 * - [PlayerArmorChangeEvent] 覆盖传统胸甲物品。
 *
 * 不在这里直接计算容量：统一交给 [com.example.bag.service.BagCapacityService]，
 * 避免多个入口重复实现掉落/占位逻辑。延迟一个 tick 是为了让装备交换后的
 * 物品栏状态稳定下来，再由 `BagPlugin.scheduleSync` 去重。
 */
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
