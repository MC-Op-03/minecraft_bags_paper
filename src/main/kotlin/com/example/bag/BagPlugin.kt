package com.example.bag

import com.example.bag.bag.BagItemFactory
import com.example.bag.bag.BagItemProvider
import com.example.bag.bag.BagItemResolver
import com.example.bag.bag.BagItemService
import com.example.bag.bag.VanillaBagItemProvider
import com.example.bag.command.BagCommand
import com.example.bag.config.BagConfig
import com.example.bag.config.ConfigLoader
import com.example.bag.listener.EquipmentListener
import com.example.bag.listener.InventoryGuardListener
import com.example.bag.listener.LifecycleListener
import com.example.bag.service.BagCapacityService
import com.example.bag.service.CapacityTracker
import com.example.bag.service.InventoryFitService
import com.example.bag.service.SpeedModifierService
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

/**
 * 插件入口与运行期装配。
 *
 * 负责：
 * - 加载/校验配置；
 * - 装配原版 provider、可选 CraftEngine provider、容量/移速服务和监听器；
 * - 启动每 20 tick 的兜底同步任务；
 * - 处理 `/bag reload` 和插件停用清理。
 *
 * 线程约束：
 * - 所有 Bukkit 状态（物品栏、玩家属性、掉落）只在主线程修改；
 * - 本类没有异步任务；scheduler 仅用于延迟回主线程和周期任务。
 */
class BagPlugin : JavaPlugin() {
    /** 当前配置快照；重载时整体替换。 */
    lateinit var bagConfig: BagConfig
        private set

    /** 原版回退背包物品构建器；占位物也由它构建。 */
    lateinit var itemFactory: BagItemFactory
        private set

    /** 原版回退物品的 PDC 读取器。 */
    lateinit var itemResolver: BagItemResolver
        private set

    /** 对业务层暴露的统一物品服务（CE 优先，原版回退）。 */
    lateinit var bagItemService: BagItemService
        private set

    /** 允许槽位放置模拟。 */
    lateinit var inventoryFit: InventoryFitService
        private set

    /** 容量、占位和降级掉落的核心服务。 */
    lateinit var capacityService: BagCapacityService
        private set

    /** 移速惩罚服务。 */
    lateinit var speedModifierService: SpeedModifierService
        private set

    /** 物品栏守卫监听器；生命周期清理时也会调用它释放冷却表。 */
    lateinit var inventoryGuardListener: InventoryGuardListener
        private set

    /** 每名玩家上一次同步的行数。 */
    val capacityTracker = CapacityTracker()

    /**
     * 已经排队等待下一次 tick 同步的玩家。
     *
     * 装备事件可能同时触发 ArmorChange 和 EquipmentChanged，使用 Set 去重，
     * 保证每个玩家每 tick 最多同步一次。
     */
    private val pendingSyncs = HashSet<UUID>()
    private var guardTask: BukkitTask? = null

    override fun onEnable() {
        saveDefaultConfig()
        // 配置错误时直接禁用插件；不要在错误配置下继续修改玩家物品。
        bagConfig =
            try {
                ConfigLoader.load(this)
            } catch (exception: Exception) {
                logger.log(Level.SEVERE, "背包配置加载失败，插件将被禁用", exception)
                server.pluginManager.disablePlugin(this)
                return
            }

        itemFactory = BagItemFactory(this)
        itemResolver = BagItemResolver(this)
        bagItemService = BagItemService(VanillaBagItemProvider(itemFactory, itemResolver), this)
        // CE 是可选的；通过反射加载，CE 不存在时不会加载 CE 类。
        bagItemService.setCraftEngine(loadCraftEngineProvider())
        inventoryFit = InventoryFitService()
        speedModifierService = SpeedModifierService(this)
        capacityService =
            BagCapacityService(
                plugin = this,
                itemFactory = itemFactory,
                itemService = bagItemService,
                tracker = capacityTracker,
                inventoryFit = inventoryFit,
                speedModifierService = speedModifierService,
            )
        inventoryGuardListener = InventoryGuardListener(this)

        server.pluginManager.registerEvents(EquipmentListener(this), this)
        server.pluginManager.registerEvents(inventoryGuardListener, this)
        server.pluginManager.registerEvents(LifecycleListener(this), this)

        val command = getCommand("bag")
        if (command == null) {
            logger.severe("plugin.yml 缺少 bag 命令声明")
        } else {
            val executor = BagCommand(this)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }

        // 兜底任务：处理其他插件/命令绕过事件直接写物品栏、或漏掉的装备变化。
        guardTask =
            server.scheduler.runTaskTimer(
                this,
                Runnable {
                    for (player in server.onlinePlayers) {
                        try {
                            capacityService.sync(player)
                        } catch (exception: Exception) {
                            logger.log(Level.WARNING, "同步玩家 ${player.name} 的背包失败", exception)
                        }
                    }
                },
                GUARD_PERIOD_TICKS,
                GUARD_PERIOD_TICKS,
            )

        // 插件热重载/启用时可能已有在线玩家，延迟一 tick 后初始化。
        server.scheduler.runTask(
            this,
            Runnable {
                for (player in server.onlinePlayers) {
                    try {
                        capacityService.syncInitial(player)
                    } catch (exception: Exception) {
                        logger.log(Level.WARNING, "初始化玩家 ${player.name} 的背包失败", exception)
                    }
                }
            },
        )
    }

    override fun onDisable() {
        guardTask?.cancel()
        guardTask = null

        // 停用时只清理不能持久化的占位物和 transient 移速，不丢真实物品。
        if (::capacityService.isInitialized) {
            for (player in server.onlinePlayers) {
                try {
                    capacityService.clearPlaceholders(player)
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "清理玩家 ${player.name} 的占位物品失败", exception)
                }
            }
        }
        if (::speedModifierService.isInitialized) {
            for (player in server.onlinePlayers) {
                try {
                    speedModifierService.clear(player)
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "清理玩家 ${player.name} 的移速修正失败", exception)
                }
            }
        }

        capacityTracker.clear()
        pendingSyncs.clear()
    }

    /**
     * 重载 `config.yml`。
     *
     * 配置成功后才替换 [bagConfig]；失败时保留旧快照并返回 false。
     * 重载后重新装配 CE provider，并同步所有在线玩家（容量/占位/移速）。
     *
     * @return 是否成功重载
     */
    fun reloadBagConfig(): Boolean =
        try {
            reloadConfig()
            val newConfig = ConfigLoader.load(this)
            bagConfig = newConfig
            bagItemService.setCraftEngine(loadCraftEngineProvider())
            for (player in server.onlinePlayers) {
                try {
                    capacityService.clearPlaceholders(player)
                    capacityService.sync(player)
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "重载后同步玩家 ${player.name} 的背包失败", exception)
                }
            }
            true
        } catch (exception: Exception) {
            logger.log(Level.SEVERE, "背包配置重载失败", exception)
            false
        }

    /**
     * 在下一个 tick 同步一次玩家背包。
     *
     * 通过 [pendingSyncs] 去重；适合装备变化等需要等物品栏稳定的事件。
     */
    fun scheduleSync(player: Player) {
        if (!player.isOnline) return
        val playerId = player.uniqueId
        if (!pendingSyncs.add(playerId)) return

        server.scheduler.runTask(
            this,
            Runnable {
                pendingSyncs.remove(playerId)
                if (player.isOnline) {
                    try {
                        capacityService.sync(player)
                    } catch (exception: Exception) {
                        logger.log(Level.WARNING, "同步玩家 ${player.name} 的背包失败", exception)
                    }
                }
            },
        )
    }

    /** 玩家退出/死亡时移除待执行同步标记。 */
    fun clearPendingSync(playerId: UUID) {
        pendingSyncs.remove(playerId)
    }

    /**
     * 按需加载 CraftEngine provider。
     *
     * 这里使用 `Class.forName` 而不是直接 new，原因：
     * CE API 是 compileOnly 依赖，CE 未安装时该 provider 类不应被加载；
     * 反射包裹可以捕获 NoClassDefFoundError/版本不兼容并回退原版。
     */
    private fun loadCraftEngineProvider(): BagItemProvider? {
        if (!bagConfig.craftEngine.enabled) {
            logger.info("CraftEngine 集成已在配置中禁用，使用原版物品")
            return null
        }
        val craftEngine = server.pluginManager.getPlugin("CraftEngine")
        if (craftEngine == null || !craftEngine.isEnabled) {
            logger.info("CraftEngine 未启用，使用原版物品回退")
            return null
        }
        return try {
            val providerClass = Class.forName("com.example.bag.craftengine.CraftEngineBagItemProvider")
            val provider =
                providerClass
                    .getConstructor(BagPlugin::class.java)
                    .newInstance(this) as BagItemProvider
            logger.info("CraftEngine 集成已启用")
            provider
        } catch (exception: Throwable) {
            logger.log(Level.WARNING, "CraftEngine 集成加载失败，使用原版物品回退", exception)
            null
        }
    }

    private companion object {
        /** 兜底同步周期：20 tick = 1 秒。 */
        const val GUARD_PERIOD_TICKS = 20L
    }
}
