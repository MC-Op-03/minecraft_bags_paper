package com.example.bag.config

import com.example.bag.bag.BagTier
import net.kyori.adventure.text.Component
import org.bukkit.Material

/**
 * 插件运行期配置快照。
 *
 * 配置只在启用/重载时整体替换，避免运行中混用新旧字段。
 * 这个对象是只读数据；修改配置后通过 [com.example.bag.BagPlugin.reloadBagConfig] 替换。
 */
data class BagConfig(
    /** 每个等级的原版回退物品外观。三个等级必须齐全。 */
    val tiers: Map<BagTier, BagTierAppearance>,
    /** 锁定槽占位物品的外观。 */
    val placeholder: PlaceholderAppearance,
    /** 玩家提示消息。 */
    val messages: Messages,
    /** CraftEngine 集成开关与物品 ID。 */
    val craftEngine: CraftEngineConfig,
    /** 移速惩罚参数。 */
    val debuffs: DebuffConfig,
) {
    /** 取指定等级的外观；缺失时抛出异常（配置加载阶段已校验齐全）。 */
    fun appearance(tier: BagTier): BagTierAppearance = tiers.getValue(tier)
}

/**
 * CraftEngine 集成配置。
 *
 * [items] 使用 CE 的 `namespace:path` 物品 ID，而不是 Bukkit Material。
 * 配置中的 ID 必须与 CE pack 的 `configuration/items.yml` 一致。
 */
data class CraftEngineConfig(
    val enabled: Boolean,
    val items: Map<BagTier, String>,
)

/**
 * 移速惩罚参数，单位为 `AttributeModifier.Operation.ADD_SCALAR` 的倍率。
 *
 * 例如 `-0.15` 表示基础速度乘以 `1 - 0.15`。
 */
data class DebuffConfig(
    val mediumSpeed: Double,
    val largeSpeed: Double,
) {
    /**
     * 根据等级返回移速修正值。
     *
     * 小背包和无背包返回 `0.0`，调用方据此移除 modifier。
     */
    fun speedFor(tier: BagTier?): Double =
        when (tier) {
            BagTier.MEDIUM -> mediumSpeed
            BagTier.LARGE -> largeSpeed
            else -> 0.0
        }
}

/**
 * 一个等级的原版回退物品外观。
 *
 * CE 正常工作时，这些字段只用于无 CE 回退或旧物品；CE 物品的外观由 CE pack 决定。
 */
data class BagTierAppearance(
    val material: Material,
    val customModelData: Int?,
    val name: Component,
    val lore: List<Component>,
)

/**
 * 锁定槽占位物品外观。
 *
 * 真正识别占位物品依赖 PDC，不依赖这里的材质或名称。
 */
data class PlaceholderAppearance(
    val material: Material,
    val name: Component,
    val lore: List<Component>,
)

/**
 * 玩家可见提示。全部使用 MiniMessage 解析后的 Adventure Component。
 */
data class Messages(
    val lockedSlot: Component,
    val capacityShrunk: Component,
    val noSpace: Component,
)
