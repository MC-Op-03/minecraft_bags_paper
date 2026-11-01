package com.example.bag.service

import com.example.bag.BagPlugin
import com.example.bag.bag.BagItemFactory
import com.example.bag.bag.BagItemService
import com.example.bag.bag.StorageLayout
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * 背包系统的统一同步服务。
 *
 * 每次同步完成四件事：
 * 1. 从胸甲栏解析当前 tier；
 * 2. 与上次行数比较，降级时丢弃新锁定行的真实物品；
 * 3. 把锁定槽中的异常物品移出，并补齐占位物品；
 * 4. 更新移速 modifier。
 *
 * 线程约束：所有方法必须在主线程调用。物品栏、掉落、属性修饰符都不是线程安全的。
 *
 * 不变量：
 * - 锁定槽要么为空，要么是 `bag:placeholder` 占位物；
 * - 降级/卸下导致的物品必须掉落，不自动搬回可用槽；
 * - 外部写入锁定槽的异常物品优先移动到可用槽，放不下才掉落；
 * - 占位物不能出现在可用槽、不能掉落、不能被玩家保存。
 */
class BagCapacityService(
    private val plugin: BagPlugin,
    private val itemFactory: BagItemFactory,
    private val itemService: BagItemService,
    private val tracker: CapacityTracker,
    private val inventoryFit: InventoryFitService,
    private val speedModifierService: SpeedModifierService,
) {
    /**
     * 只读计算玩家当前解锁行数。
     *
     * 供事件监听器提前判断锁定槽，不触发任何物品移动。
     */
    fun rowsOf(player: Player): Int {
        val chestplate = player.inventory.chestplate
        return itemService.tierOf(chestplate)?.rows ?: 0
    }

    /**
     * 玩家首次进入/重生后的初始化同步。
     *
     * 先把当前容量写入 tracker 再 sync，这样第一次 sync 不会把
     * “未知的旧容量”误判成降级而丢物品。
     */
    fun syncInitial(player: Player) {
        if (!player.isOnline) return
        tracker.set(player.uniqueId, rowsOf(player))
        sync(player)
    }

    /**
     * 完成一次完整同步。
     *
     * 必须在主线程执行；通常由装备事件、生命周期事件、交互后校正、
     * 每 20 tick 兜底任务或 `/bag reload` 调用。
     */
    fun sync(player: Player) {
        if (!player.isOnline) return

        val tier = itemService.tierOf(player.inventory.chestplate)
        val newRows = tier?.rows ?: 0
        val previousRows = tracker.get(player.uniqueId)
        if (previousRows != null && newRows < previousRows) {
            // 只丢弃“本次降级新锁定”的槽位；之前已经锁定的槽位由 heal 处理。
            val dropped = dropSlots(player, StorageLayout.newlyLockedSlots(previousRows, newRows))
            if (dropped > 0) {
                player.sendActionBar(plugin.bagConfig.messages.capacityShrunk)
            }
        }

        // 先处理真实物品，再放占位物；避免覆盖/丢失异常写入的物品。
        healLockedSlots(player, newRows)
        applyPlaceholders(player, newRows)
        speedModifierService.update(player, tier)
        tracker.set(player.uniqueId, newRows)
    }

    /**
     * 清理玩家物品栏中所有占位物。
     *
     * 用于死亡、断线、插件停用和重载；不会动真实物品。
     */
    fun clearPlaceholders(player: Player) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (itemFactory.isPlaceholder(inventory.getItem(slot))) {
                inventory.setItem(slot, null)
            }
        }
    }

    /**
     * 通过命令发放物品，只写入快捷栏和可用存储区。
     *
     * @return `true` 表示全部放入；`false` 表示有剩余被掉落在脚下
     */
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

    /**
     * 丢弃指定槽位范围中的真实物品。
     *
     * 占位物只清空、不计数；掉落失败会尝试放回原槽，等待下次 heal/sync。
     *
     * @return 实际成功掉落的物品堆数（用于决定是否提示玩家）
     */
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

    /**
     * 把锁定槽中出现的真实物品移动到可用槽，放不下才掉落。
     *
     * 这是异常路径的修复逻辑，不负责正常的降级丢弃（后者由 dropSlots 完成）。
     */
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

    /**
     * 保证锁定槽中有占位物、可用槽中没有占位物。
     *
     * 如果锁定槽仍有真实物品（说明 heal 失败），不要用占位物覆盖它；
     * 记录警告并等待下一次同步重试，避免物品丢失。
     */
    private fun applyPlaceholders(
        player: Player,
        rows: Int,
    ) {
        val inventory = player.inventory
        val lockedSlots = StorageLayout.lockedSlots(rows)

        // 先移除位于可用槽/快捷栏/盔甲/副手的占位物。
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (itemFactory.isPlaceholder(item) && slot !in lockedSlots) {
                inventory.setItem(slot, null)
            }
        }

        // 再补齐锁定槽；已有占位物不重复创建，减少无意义的数据包和卡顿。
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

    /**
     * 在玩家脚下自然掉落物品；失败时记录并返回 false，由调用方决定是否恢复槽位。
     */
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
