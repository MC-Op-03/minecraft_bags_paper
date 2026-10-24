package com.example.bag.config

import com.example.bag.bag.BagTier
import net.kyori.adventure.text.Component
import org.bukkit.Material

data class BagConfig(
    val tiers: Map<BagTier, BagTierAppearance>,
    val placeholder: PlaceholderAppearance,
    val messages: Messages,
) {
    fun appearance(tier: BagTier): BagTierAppearance = tiers.getValue(tier)
}

data class BagTierAppearance(
    val material: Material,
    val customModelData: Int?,
    val name: Component,
    val lore: List<Component>,
)

data class PlaceholderAppearance(
    val material: Material,
    val name: Component,
    val lore: List<Component>,
)

data class Messages(
    val lockedSlot: Component,
    val capacityShrunk: Component,
    val noSpace: Component,
)
