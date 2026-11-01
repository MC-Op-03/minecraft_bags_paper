package com.example.bag.bag

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * 读取原版回退物品的 `bag:tier` 标记。
 *
 * 只读取 PDC，不修改物品，也不依赖显示名称/Lore/材质。
 * CraftEngine 物品的识别在 `CraftEngineBagItemProvider` 中完成；
 * [BagItemService] 会先尝试 CE，再回退到这里。
 */
class BagItemResolver(
    plugin: Plugin,
) {
    private val tierKey = NamespacedKey(plugin, "tier")

    /**
     * @return 物品等级；空物品、没有 PDC 标记或 ID 未知时返回 `null`
     */
    fun tierOf(item: ItemStack?): BagTier? {
        if (item == null || item.type.isAir) return null
        val container = item.persistentDataContainer
        if (!container.has(tierKey, PersistentDataType.STRING)) return null
        return BagTier.fromId(container.get(tierKey, PersistentDataType.STRING))
    }
}
