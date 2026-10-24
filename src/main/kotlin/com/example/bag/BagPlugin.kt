package com.example.bag

import com.example.bag.bag.BagItemFactory
import com.example.bag.bag.BagItemResolver
import com.example.bag.command.BagCommand
import com.example.bag.config.BagConfig
import com.example.bag.config.ConfigLoader
import com.example.bag.listener.EquipmentListener
import com.example.bag.listener.InventoryGuardListener
import com.example.bag.listener.LifecycleListener
import com.example.bag.service.BagCapacityService
import com.example.bag.service.CapacityTracker
import com.example.bag.service.InventoryFitService
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

class BagPlugin : JavaPlugin() {
    lateinit var bagConfig: BagConfig
        private set
    lateinit var itemFactory: BagItemFactory
        private set
    lateinit var itemResolver: BagItemResolver
        private set
    lateinit var inventoryFit: InventoryFitService
        private set
    lateinit var capacityService: BagCapacityService
        private set
    lateinit var inventoryGuardListener: InventoryGuardListener
        private set

    val capacityTracker = CapacityTracker()

    private val pendingSyncs = HashSet<UUID>()
    private var guardTask: BukkitTask? = null

    override fun onEnable() {
        saveDefaultConfig()
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
        inventoryFit = InventoryFitService()
        capacityService =
            BagCapacityService(
                plugin = this,
                itemFactory = itemFactory,
                itemResolver = itemResolver,
                tracker = capacityTracker,
                inventoryFit = inventoryFit,
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

        if (::capacityService.isInitialized) {
            for (player in server.onlinePlayers) {
                try {
                    capacityService.clearPlaceholders(player)
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "清理玩家 ${player.name} 的占位物品失败", exception)
                }
            }
        }

        capacityTracker.clear()
        pendingSyncs.clear()
    }

    fun reloadBagConfig(): Boolean =
        try {
            reloadConfig()
            val newConfig = ConfigLoader.load(this)
            bagConfig = newConfig
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

    fun clearPendingSync(playerId: UUID) {
        pendingSyncs.remove(playerId)
    }

    private companion object {
        const val GUARD_PERIOD_TICKS = 20L
    }
}
