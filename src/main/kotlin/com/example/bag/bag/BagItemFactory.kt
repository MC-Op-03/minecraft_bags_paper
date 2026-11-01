package com.example.bag.bag

import com.example.bag.BagPlugin
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 原版回退物品与锁定槽占位物品的构建器。
 *
 * 这个类只构建“物品外观/元数据”，不决定容量和移速；
 * 容量规则在 [StorageLayout]，移速在 [com.example.bag.service.SpeedModifierService]。
 *
 * 背包等级通过 PDC 写入 [tierKey]；占位物品通过 [placeholderKey] 标记。
 * 不要把显示名称、Lore 或材质作为识别依据，因为这些都可以被玩家修改。
 */
class BagItemFactory(
    private val plugin: BagPlugin,
) {
    /** 原版背包物品的等级标记。 */
    val tierKey: NamespacedKey = NamespacedKey(plugin, "tier")

    /** 锁定槽占位物品的标记。 */
    val placeholderKey: NamespacedKey = NamespacedKey(plugin, "placeholder")

    /**
     * 创建一个原版回退背包物品。
     *
     * 注意：
     * - 材质/名称/Lore/CustomModelData 来自 `config.yml`；
     * - 使用空 `ATTRIBUTE_MODIFIERS` 去掉胸甲材质自带的护甲属性；
     * - `setMaxStackSize(1)` 保证背包不会堆叠。
     */
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
        // 不使用胸甲材质默认的护甲属性；背包只是装备槽载体。
        item.setData(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.itemAttributes().build(),
        )
        return item
    }

    /**
     * 创建一个锁定槽视觉占位物品。
     *
     * 占位物品是真实物品：它占据锁定槽，阻止原版把其他物品放进去，
     * 同时也是客户端可见的“未解锁”提示。PDC 标记是唯一识别依据。
     * 最大堆叠为 1，避免与普通同材质物品合并。
     */
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

    /**
     * 判断物品是否带占位标记。
     *
     * 不依赖材质，因此调整 `placeholder.material` 不会破坏识别。
     */
    fun isPlaceholder(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        return item.persistentDataContainer.has(placeholderKey, PersistentDataType.BYTE)
    }

    private companion object {
        /** PDC byte 标记值；只用于存在性判断，具体值不重要。 */
        const val PLACEHOLDER_VALUE: Byte = 1
    }
}
