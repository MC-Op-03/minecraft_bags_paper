package com.example.bag.command

import com.example.bag.BagPlugin
import com.example.bag.bag.BagTier
import com.example.bag.bag.StorageLayout
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale

/**
 * `/bag` 管理命令。
 *
 * 子命令：
 * - `give <player> <small|medium|large>`：按当前内容 provider 生成背包并发放；
 * - `info [player]`：查看容量、锁定槽和移速修正；
 * - `reload`：重载配置并同步在线玩家。
 *
 * 命令只做参数校验和调用服务，不直接修改物品栏。
 * 权限：`bag.admin`，默认仅 OP。
 */
class BagCommand(
    private val plugin: BagPlugin,
) : CommandExecutor,
    TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("你没有权限使用该命令。", NamedTextColor.RED))
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("用法：/bag <give|info|reload>", NamedTextColor.YELLOW))
            return true
        }

        when (args[0].lowercase(Locale.ROOT)) {
            "give" -> give(sender, args)
            "info" -> info(sender, args)
            "reload" -> reload(sender)
            else -> sender.sendMessage(Component.text("未知子命令：${args[0]}", NamedTextColor.RED))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (!sender.hasPermission(PERMISSION)) return emptyList()

        return when (args.size) {
            1 -> {
                listOf("give", "info", "reload").filter { it.startsWith(args[0], ignoreCase = true) }
            }

            2 -> {
                if (args[0].equals("give", true) || args[0].equals("info", true)) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            3 -> {
                if (args[0].equals("give", true)) {
                    BagTier.entries.map { it.id }.filter { it.startsWith(args[2], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            else -> {
                emptyList()
            }
        }
    }

    /**
     * 发放背包。
     *
     * - 目标必须在线；
     * - 等级必须是 [BagTier] 中的稳定 ID；
     * - 物品由 [BagItemService] 创建（CE 优先，原版回退）；
     * - 实际放置走 [BagCapacityService.give]，只会进入允许槽，放不下则掉落。
     */
    private fun give(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (args.size != 3) {
            sender.sendMessage(Component.text("用法：/bag give <player> <small|medium|large>", NamedTextColor.YELLOW))
            return
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线：${args[1]}", NamedTextColor.RED))
            return
        }

        val tier = BagTier.fromId(args[2].lowercase(Locale.ROOT))
        if (tier == null) {
            sender.sendMessage(Component.text("未知背包等级：${args[2]}", NamedTextColor.RED))
            return
        }

        val item = plugin.bagItemService.createBag(tier)
        if (item == null) {
            sender.sendMessage(Component.text("无法创建该背包物品；请检查 CraftEngine 配置或控制台日志。", NamedTextColor.RED))
            return
        }
        val added = plugin.capacityService.give(target, item)
        val tierName = plugin.bagConfig.appearance(tier).name
        val message =
            Component
                .text()
                .append(Component.text("已给予 ", NamedTextColor.GREEN))
                .append(Component.text(target.name, NamedTextColor.WHITE))
                .append(Component.text(" 一个 ", NamedTextColor.GREEN))
                .append(tierName)
                .append(Component.text("。", NamedTextColor.GREEN))
                .build()
        sender.sendMessage(message)
        // 当发送者就是目标时不重复发同一句话。
        if (sender !== target) {
            target.sendMessage(message)
        }
        if (!added) {
            sender.sendMessage(Component.text("目标物品栏已满，背包掉落在其脚下。", NamedTextColor.YELLOW))
        }
    }

    /**
     * 查看目标玩家的背包状态。
     *
     * 该命令只读：不触发容量校正，也不修改物品或属性。
     */
    private fun info(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        val target =
            when {
                args.size >= 2 -> Bukkit.getPlayerExact(args[1])
                sender is Player -> sender
                else -> null
            }
        if (target == null) {
            sender.sendMessage(Component.text("用法：/bag info [player]", NamedTextColor.YELLOW))
            return
        }

        val rows = plugin.capacityService.rowsOf(target)
        val tier = plugin.bagItemService.tierOf(target.inventory.chestplate)
        val allowed = StorageLayout.allowedStorageSlots(rows)
        val locked = StorageLayout.lockedSlots(rows)
        val speedPenalty = plugin.bagConfig.debuffs.speedFor(tier)
        val speedText = if (speedPenalty == 0.0) "无" else "${(speedPenalty * 100).toInt()}%"
        sender.sendMessage(
            Component
                .text()
                .append(Component.text("${target.name}：", NamedTextColor.WHITE))
                .append(Component.text(if (tier == null) "无背包" else "背包 ${tier.id}", NamedTextColor.GRAY))
                .append(Component.text("，存储行 $rows", NamedTextColor.GRAY))
                .append(Component.text("，可用存储槽 ${rangeText(allowed)}", NamedTextColor.GRAY))
                .append(Component.text("，锁定槽 ${rangeText(locked)}", NamedTextColor.GRAY))
                .append(Component.text("，移速修正 $speedText", NamedTextColor.GRAY))
                .build(),
        )
    }

    /** 空范围显示为“无”，避免输出 `1..0` 之类的内部表示。 */
    private fun rangeText(range: IntRange): String = if (range.isEmpty()) "无" else range.toString()

    /** 重载配置；具体失败原因由 [BagPlugin.reloadBagConfig] 写入控制台。 */
    private fun reload(sender: CommandSender) {
        if (plugin.reloadBagConfig()) {
            sender.sendMessage(Component.text("配置已重载。", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("配置重载失败，请查看控制台。", NamedTextColor.RED))
        }
    }

    private companion object {
        const val PERMISSION = "bag.admin"
    }
}
