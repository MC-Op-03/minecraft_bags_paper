package com.example.bag.bag

import org.bukkit.inventory.ItemStack

/**
 * 不依赖 CraftEngine 的原版背包物品实现。
 *
 * 物品使用 `config.yml` 中的 `tiers` 材质、名称和 CustomModelData 创建，
 * 等级写入 PDC `bag:tier`。这个 provider 是 CE 未安装时的回退路径，
 * 也用于兼容 v0.1.0 已存在的物品。
 */
class VanillaBagItemProvider(
    private val itemFactory: BagItemFactory,
    private val itemResolver: BagItemResolver,
) : BagItemProvider {
    override fun createBag(tier: BagTier): ItemStack = itemFactory.createBag(tier)

    override fun tierOf(item: ItemStack?): BagTier? = itemResolver.tierOf(item)
}
