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

/**
 * 玩家生命周期与背包状态同步。
 *
 * 关键不变量：
 * - 占位物品不能进入死亡掉落列表，也不能被保存到玩家数据；
 * - 移速 modifier 是 transient，断线/死亡/停用时必须移除；
 * - 加入/重生/换世界后重新从胸甲栏读取真实容量。
 *
 * 所有事件都在主线程处理；涉及物品栏的延迟任务使用 scheduler 回主线程。
 */
class LifecycleListener(
    private val plugin: BagPlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        // 延迟一个 tick，确保玩家物品栏已完整加载。
        plugin.scheduleSync(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        // 死亡时 tracker 已移除；重生先以当前胸甲重置基线，避免误判降级。
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
        // 原版物品栏跨维度保留，但副本/插件可能重建属性，重新同步一次。
        plugin.scheduleSync(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        // 占位物品不能被死亡掉落或被其他插件当作普通物品处理。
        event.drops.removeIf { plugin.itemFactory.isPlaceholder(it) }
        plugin.speedModifierService.clear(player)
        plugin.capacityService.clearPlaceholders(player)
        plugin.capacityTracker.remove(player.uniqueId)
        plugin.clearPendingSync(player.uniqueId)
        plugin.inventoryGuardListener.cleanup(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 退出前先同步一次，确保最后一次装备变化的溢出规则生效。
        plugin.capacityService.sync(player)
        // 然后清掉不能持久化的占位和 transient 移速。
        plugin.speedModifierService.clear(player)
        plugin.capacityService.clearPlaceholders(player)
        plugin.capacityTracker.remove(player.uniqueId)
        plugin.clearPendingSync(player.uniqueId)
        plugin.inventoryGuardListener.cleanup(player.uniqueId)
    }
}
