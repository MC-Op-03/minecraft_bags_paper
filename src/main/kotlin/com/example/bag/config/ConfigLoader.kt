package com.example.bag.config

import com.example.bag.bag.BagTier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

/**
 * 将 `config.yml` 转换为不可变 [BagConfig]。
 *
 * 所有校验集中在加载阶段：缺失/非法配置会抛异常，由
 * [com.example.bag.BagPlugin] 禁用插件或拒绝重载。运行期服务不再重复校验，
 * 避免错误配置在玩家操作过程中产生半生效状态。
 *
 * 这里不做 `saveConfig()`，不会改写用户已有配置。
 */
object ConfigLoader {
    private val miniMessage = MiniMessage.miniMessage()

    /**
     * 读取并校验完整配置。
     *
     * 三个 tier 必须存在；CE 物品 ID、材质、移速和 MiniMessage 都会在这里验证。
     */
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
            craftEngine = loadCraftEngine(config.getConfigurationSection("craftengine")),
            debuffs = loadDebuffs(config.getConfigurationSection("debuffs")),
        )
    }

    /**
     * 读取移速惩罚。
     *
     * 默认值来自代码常量；缺失配置节时保持兼容。
     * 只允许 `-1.0..0.0`，防止配置错误变成加速或完全无法移动。
     */
    private fun loadDebuffs(section: ConfigurationSection?): DebuffConfig {
        val medium = section?.getDouble("medium-speed", DEFAULT_MEDIUM_SPEED) ?: DEFAULT_MEDIUM_SPEED
        val large = section?.getDouble("large-speed", DEFAULT_LARGE_SPEED) ?: DEFAULT_LARGE_SPEED
        require(medium.isFinite() && medium in -1.0..0.0) {
            "debuffs.medium-speed 必须是 -1.0 到 0.0 之间的有限数"
        }
        require(large.isFinite() && large in -1.0..0.0) {
            "debuffs.large-speed 必须是 -1.0 到 0.0 之间的有限数"
        }
        return DebuffConfig(mediumSpeed = medium, largeSpeed = large)
    }

    /**
     * 读取 CE provider 开关与三个 CE 物品 ID。
     *
     * ID 必须符合 `namespace:path`；这里只校验格式，物品是否真的存在于
     * CE 由运行时 provider 检查并回退。
     */
    private fun loadCraftEngine(section: ConfigurationSection?): CraftEngineConfig {
        val itemsSection = section?.getConfigurationSection("items")
        val items =
            BagTier.entries.associateWith { tier ->
                val value = itemsSection?.getString(tier.id) ?: DEFAULT_CRAFTENGINE_ITEMS.getValue(tier)
                require(ITEM_ID_PATTERN.matches(value)) {
                    "craftengine.items.${tier.id} 不是有效的 CraftEngine 物品 ID：$value"
                }
                value
            }
        return CraftEngineConfig(
            enabled = section?.getBoolean("enabled", true) ?: true,
            items = items,
        )
    }

    /**
     * 读取一个等级的原版回退外观。
     *
     * `tiers.<id>` 必须存在；名称/Lore 允许省略并使用代码默认值，
     * 但材质必须有效且可作为物品。
     */
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

    /**
     * 读取占位物外观。
     *
     * 占位物识别依赖 PDC，不依赖这里的材质；这里只决定客户端看到的外观。
     */
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

    /** 读取玩家提示；缺失时使用内置中文默认值。 */
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

    /**
     * 解析 Bukkit Material，兼容大小写；拒绝非物品材质。
     */
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

    /**
     * 解析 MiniMessage，并在失败时带上配置路径，方便定位错误。
     */
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

    private const val DEFAULT_MEDIUM_SPEED = -0.15
    private const val DEFAULT_LARGE_SPEED = -0.30
    private val DEFAULT_CRAFTENGINE_ITEMS =
        mapOf(
            BagTier.SMALL to "bag:small_bag",
            BagTier.MEDIUM to "bag:medium_bag",
            BagTier.LARGE to "bag:large_bag",
        )
    private val ITEM_ID_PATTERN = Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$")
}
