package com.example.bag.bag

import com.example.bag.BagPlugin
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * 背包物品的对外门面。
 *
 * 读取/创建顺序：
 * 1. 如果当前存在 CraftEngine provider，先尝试 CE；
 * 2. CE 无法处理（物品不存在、返回 `null` 或抛异常）时回退到原版 provider。
 *
 * 这样业务代码不需要知道服务器是否安装 CE，也不会因为 CE 配置缺失
 * 而完全无法发放背包。CE provider 通过反射在 [BagPlugin] 中按需装载，
 * 避免 CE 未安装时加载 CE 类。
 */
class BagItemService(
    private val vanilla: BagItemProvider,
    private val plugin: BagPlugin,
) : BagItemProvider {
    /** 当前 CE provider；为 `null` 表示未启用 CE 或 CE 加载失败。 */
    private var craftEngine: BagItemProvider? = null

    /** 供命令或诊断使用的状态查询。 */
    val usingCraftEngine: Boolean
        get() = craftEngine != null

    /**
     * 替换 CE provider。
     *
     * 由 [BagPlugin] 在启用和配置重载时调用；传入 `null` 表示退回原版。
     */
    fun setCraftEngine(provider: BagItemProvider?) {
        craftEngine = provider
    }

    override fun createBag(tier: BagTier): ItemStack? =
        craftEngine?.let { provider ->
            try {
                provider.createBag(tier)
            } catch (exception: Exception) {
                // CE 构建失败不应该阻止整个背包系统工作，记录后回退。
                plugin.logger.log(Level.WARNING, "创建 CraftEngine 背包物品失败：$tier", exception)
                null
            }
        } ?: vanilla.createBag(tier)

    override fun tierOf(item: ItemStack?): BagTier? =
        craftEngine?.let { provider ->
            try {
                provider.tierOf(item)
            } catch (exception: Exception) {
                // tierOf 在点击/拾取路径上被频繁调用，使用 FINE 避免刷屏。
                plugin.logger.log(Level.FINE, "读取 CraftEngine 背包物品失败", exception)
                null
            }
        } ?: vanilla.tierOf(item)
}
