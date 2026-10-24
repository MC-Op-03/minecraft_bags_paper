package com.example.bag.bag

import com.example.bag.BagPlugin
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class BagItemFactory(
    private val plugin: BagPlugin,
) {
    val tierKey: NamespacedKey = NamespacedKey(plugin, "tier")
    val placeholderKey: NamespacedKey = NamespacedKey(plugin, "placeholder")

    fun createBag(tier: BagTier): ItemStack {
        val appearance = plugin.bagConfig.appearance(tier)
        val item = ItemStack(appearance.material)
        item.editMeta { meta ->
            meta.displayName(appearance.name)
            meta.lore(appearance.lore)
            meta.setUnbreakable(true)
            meta.setMaxStackSize(1)
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES)
            meta.persistentDataContainer.set(tierKey, PersistentDataType.STRING, tier.id)
        }
        appearance.customModelData?.let { value ->
            item.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addFloat(value.toFloat()),
            )
        }
        item.setData(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.itemAttributes().build(),
        )
        return item
    }

    fun createPlaceholder(): ItemStack {
        val appearance = plugin.bagConfig.placeholder
        val item = ItemStack(appearance.material)
        item.editMeta { meta ->
            meta.displayName(appearance.name)
            meta.lore(appearance.lore)
            meta.setMaxStackSize(1)
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            meta.persistentDataContainer.set(placeholderKey, PersistentDataType.BYTE, PLACEHOLDER_VALUE)
        }
        return item
    }

    fun isPlaceholder(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        return item.persistentDataContainer.has(placeholderKey, PersistentDataType.BYTE)
    }

    private companion object {
        const val PLACEHOLDER_VALUE: Byte = 1
    }
}
