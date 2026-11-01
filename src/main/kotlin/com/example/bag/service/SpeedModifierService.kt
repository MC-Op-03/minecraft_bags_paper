package com.example.bag.service

import com.example.bag.BagPlugin
import com.example.bag.bag.BagTier
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

/**
 * 管理背包移速惩罚。
 *
 * 设计约束：
 * - 只使用一个固定 key `bag:movement_speed`，确保同一玩家永远不会叠加多个
 *   插件 modifier；
 * - 使用 transient modifier，不写入玩家持久化属性；
 * - 使用 `ADD_SCALAR`：没有其他速度修正时，-0.15/-0.30 就是基础速度的
 *   -15%/-30%；
 * - 所有调用都在主线程。
 */
class SpeedModifierService(
    private val plugin: BagPlugin,
) {
    private val key = NamespacedKey(plugin, "movement_speed")

    /**
     * 根据当前胸甲栏等级更新移速修饰符。
     *
     * 同一 key 已存在且数值相同时直接返回，避免每 20 tick 重复重建；
     * 数值变化（例如 `/bag reload`）或等级变化时才替换。
     *
     * @param tier 当前装备的背包等级；`null`/小背包表示无惩罚
     */
    fun update(
        player: Player,
        tier: BagTier?,
    ) {
        val attribute = player.getAttribute(Attribute.MOVEMENT_SPEED) ?: return
        val amount = plugin.bagConfig.debuffs.speedFor(tier)
        val existing = attribute.getModifier(key)

        if (amount == 0.0) {
            if (existing != null) attribute.removeModifier(key)
            return
        }
        if (existing != null && existing.amount == amount) return

        attribute.removeModifier(key)
        attribute.addTransientModifier(
            AttributeModifier(key, amount, AttributeModifier.Operation.ADD_SCALAR),
        )
    }

    /** 移除玩家身上的背包移速 modifier。 */
    fun clear(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(key)
    }

    /** 插件停用时清理所有在线玩家。 */
    fun clearAll(players: Collection<Player>) {
        players.forEach(::clear)
    }
}
