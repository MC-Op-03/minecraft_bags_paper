package com.example.bag.bag

import org.bukkit.inventory.ItemStack

/**
 * 背包物品来源的抽象。
 *
 * 当前有两个实现：
 * - [VanillaBagItemProvider]：不依赖第三方插件，使用 PDC `bag:tier`；
 * - [com.example.bag.craftengine.CraftEngineBagItemProvider]：使用 CraftEngine 物品 ID。
 *
 * 业务层只依赖本接口，不直接依赖 CraftEngine API，这样 CE 未安装时
 * 不会触发类加载错误，也方便未来增加其他物品来源。
 *
 * 所有方法都应在服务端主线程调用。
 */
interface BagItemProvider {
    /**
     * 创建指定等级的背包物品。
     *
     * @return 创建出的物品；当前 provider 无法创建时返回 `null`，
     * 由调用方决定是否回退到其他 provider。
     */
    fun createBag(tier: BagTier): ItemStack?

    /**
     * 读取物品对应的背包等级。
     *
     * @return 等级；物品不是本 provider 的背包、为空或无法识别时返回 `null`
     */
    fun tierOf(item: ItemStack?): BagTier?
}
