package com.example.bag.craftengine

import com.example.bag.BagPlugin
import com.example.bag.bag.BagItemProvider
import com.example.bag.bag.BagTier
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * CraftEngine 背包物品适配器。
 *
 * 这个类直接引用 CraftEngine API，因此只在 CE 插件存在时由
 * [BagPlugin] 通过反射加载；CE 未安装时该类不应被类加载器加载。
 *
 * 使用的都是 CE 稳定包 `net.momirealms.craftengine.bukkit.api` 中的方法：
 * - `CraftEngineItems.byId(...)` 创建物品；
 * - `CraftEngineItems.getCustomItemId(...)` 识别物品。
 *
 * 所有方法都在主线程调用。
 */
class CraftEngineBagItemProvider(
    private val plugin: BagPlugin,
) : BagItemProvider {
    /** 每个等级只记录一次“物品不存在”警告，避免每次点击刷日志。 */
    private val missingItems = HashSet<BagTier>()

    /**
     * 按 `config.yml` 中 `craftengine.items.<tier>` 的 ID 构建 CE 物品。
     *
     * @return CE 物品；ID 缺失、CE 尚未加载或构建异常时返回 `null`，
     * 由 [com.example.bag.bag.BagItemService] 回退原版物品。
     */
    override fun createBag(tier: BagTier): ItemStack? {
        val itemId = plugin.bagConfig.craftEngine.items[tier] ?: return null
        val definition =
            CraftEngineItems.byId(itemId)
                ?: run {
                    if (missingItems.add(tier)) {
                        plugin.logger.warning("CraftEngine 物品不存在：$itemId；该等级回退原版物品")
                    }
                    return null
                }
        return try {
            definition.buildBukkitItem()
        } catch (exception: Exception) {
            plugin.logger.log(Level.WARNING, "构建 CraftEngine 物品失败：$itemId", exception)
            null
        }
    }

    /**
     * 通过 CE 自定义物品 ID 识别背包等级。
     *
     * 不读取名称/模型；CE 物品 ID 是稳定标识。
     */
    override fun tierOf(item: ItemStack?): BagTier? {
        if (item == null || item.type.isAir) return null
        val itemId = CraftEngineItems.getCustomItemId(item)?.toString() ?: return null
        return plugin.bagConfig.craftEngine.items.entries
            .firstOrNull { (_, configuredId) -> configuredId.equals(itemId, ignoreCase = true) }
            ?.key
    }
}
