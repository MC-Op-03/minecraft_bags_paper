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

/**
 * 物品栏边界守卫。
 *
 * 原版客户端不知道“锁定槽”的概念，因此必须在服务端拦截：
 * - 点击锁定槽或占位物品；
 * - 把物品拖动到锁定槽；
 * - 从其他容器 Shift 点击到没有空间的玩家物品栏；
 * - 拾取物品时进入锁定槽。
 *
 * 占位物品是第二层保护：即使某个事件被其他插件取消/绕过，锁定槽本身
 * 也已经被占位物品占据。`BagCapacityService` 的兜底任务负责处理
 * 其他插件直接写物品栏的情况。
 */
class InventoryGuardListener(
    private val plugin: BagPlugin,
) : Listener {
    /** 每个玩家的提示冷却，避免快速点击时刷 action bar。 */
    private val lastNoticeAt = HashMap<UUID, Long>()

    /**
     * 点击拦截。
     *
     * 使用 `clickedInventory === player.inventory` 而不是 raw slot，
     * 因为容器界面的 raw slot 会带顶部容器偏移；对玩家自己的物品栏，
     * `event.slot` 才是 `0..40` 的玩家槽位。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // 任何与占位物品有关的点击都拒绝，包括光标上是占位物品的情况。
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
            // 从箱子/工作台等 Shift 点击进玩家物品栏时，先模拟是否放得下。
            val item = event.currentItem
            if (item != null && !item.type.isAir && !plugin.inventoryFit.wouldFit(player.inventory, item, rows)) {
                event.isCancelled = true
                notifyNoSpace(player)
                return
            }
        }

        if (clickedPlayerInventory || event.isShiftClick) {
            // 点击后延迟校正，处理原版在允许/锁定边界上的行为差异。
            plugin.scheduleSync(player)
        }
    }

    /**
     * 拖动拦截。
     *
     * `getInventory(rawSlot)` 判断目标槽是否属于玩家物品栏；
     * `convertSlot` 将它转换成玩家物品栏索引。任何目标为锁定槽的拖动
     * 整体取消，避免一半物品被放进锁定区。
     */
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

    /**
     * 拾取拦截。
     *
     * 原版拾取会直接写入第一个可用槽；当允许槽没有空间时取消拾取，
     * 物品留在地上。`wouldFit` 只模拟快捷栏和已解锁存储区。
     */
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
        // 占位物品不应该被玩家丢出；正常流程也不会让玩家拿到占位物品。
        if (isPlaceholder(event.itemDrop.itemStack)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemSpawn(event: ItemSpawnEvent) {
        // 第三方插件或异常路径可能生成占位物品实体；直接阻止生成。
        if (isPlaceholder(event.entity.itemStack)) {
            event.isCancelled = true
        }
    }

    /** 玩家退出时清理提示冷却；避免 UUID map 长期增长。 */
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
