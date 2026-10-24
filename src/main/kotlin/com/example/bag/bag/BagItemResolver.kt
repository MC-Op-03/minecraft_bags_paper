package com.example.bag.bag

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class BagItemResolver(
    plugin: Plugin,
) {
    private val tierKey = NamespacedKey(plugin, "tier")

    fun tierOf(item: ItemStack?): BagTier? {
        if (item == null || item.type.isAir) return null
        val container = item.persistentDataContainer
        if (!container.has(tierKey, PersistentDataType.STRING)) return null
        return BagTier.fromId(container.get(tierKey, PersistentDataType.STRING))
    }
}
