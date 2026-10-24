package com.example.bag.config

import com.example.bag.bag.BagTier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

object ConfigLoader {
    private val miniMessage = MiniMessage.miniMessage()

    fun load(plugin: JavaPlugin): BagConfig {
        val config = plugin.config
        val tiers =
            BagTier.entries.associateWith { tier ->
                loadTier(config.getConfigurationSection("tiers.${tier.id}"), tier)
            }
        return BagConfig(
            tiers = tiers,
            placeholder = loadPlaceholder(config.getConfigurationSection("placeholder")),
            messages = loadMessages(config.getConfigurationSection("messages")),
        )
    }

    private fun loadTier(
        section: ConfigurationSection?,
        tier: BagTier,
    ): BagTierAppearance {
        val path = "tiers.${tier.id}"
        if (section == null) {
            throw IllegalArgumentException("缺少配置节 $path")
        }
        val material = loadMaterial(section.getString("material"), "$path.material")
        val customModelData =
            if (section.contains("custom-model-data")) {
                val value = section.getInt("custom-model-data")
                require(value >= 0) { "$path.custom-model-data 必须是非负整数" }
                value
            } else {
                null
            }
        return BagTierAppearance(
            material = material,
            customModelData = customModelData,
            name = parse(section.getString("name"), "<white>${tier.id}", "$path.name"),
            lore =
                section.getStringList("lore").mapIndexed { index, raw ->
                    parse(raw, raw, "$path.lore[$index]")
                },
        )
    }

    private fun loadPlaceholder(section: ConfigurationSection?): PlaceholderAppearance {
        val material =
            loadMaterial(
                section?.getString("material") ?: "GRAY_STAINED_GLASS_PANE",
                "placeholder.material",
            )
        return PlaceholderAppearance(
            material = material,
            name = parse(section?.getString("name"), "<dark_gray>未解锁", "placeholder.name"),
            lore =
                (section?.getStringList("lore") ?: emptyList()).mapIndexed { index, raw ->
                    parse(raw, raw, "placeholder.lore[$index]")
                },
        )
    }

    private fun loadMessages(section: ConfigurationSection?): Messages =
        Messages(
            lockedSlot =
                parse(
                    section?.getString("locked-slot"),
                    "<red>该物品栏尚未解锁。",
                    "messages.locked-slot",
                ),
            capacityShrunk =
                parse(
                    section?.getString("capacity-shrunk"),
                    "<yellow>背包容量变小，超出的物品已掉落在脚下。",
                    "messages.capacity-shrunk",
                ),
            noSpace =
                parse(
                    section?.getString("no-space"),
                    "<red>背包空间不足。",
                    "messages.no-space",
                ),
        )

    private fun loadMaterial(
        raw: String?,
        path: String,
    ): Material {
        val value = raw ?: ""
        val material =
            Material.matchMaterial(value)
                ?: Material.matchMaterial(value.uppercase(Locale.ROOT))
                ?: throw IllegalArgumentException("$path 不是有效的物品材质：$raw")
        require(material.isItem) { "$path 必须是可以作为物品的材质：$raw" }
        return material
    }

    private fun parse(
        raw: String?,
        fallback: String,
        path: String,
    ): Component {
        val text = raw ?: fallback
        return try {
            miniMessage.deserialize(text)
        } catch (exception: Exception) {
            throw IllegalArgumentException("$path 的 MiniMessage 格式无效：${exception.message}", exception)
        }
    }
}
