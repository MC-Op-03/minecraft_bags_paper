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

        val item = plugin.itemFactory.createBag(tier)
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
        if (sender !== target) {
            target.sendMessage(message)
        }
        if (!added) {
            sender.sendMessage(Component.text("目标物品栏已满，背包掉落在其脚下。", NamedTextColor.YELLOW))
        }
    }

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
        val tier = plugin.itemResolver.tierOf(target.inventory.chestplate)
        val allowed = StorageLayout.allowedStorageSlots(rows)
        val locked = StorageLayout.lockedSlots(rows)
        sender.sendMessage(
            Component
                .text()
                .append(Component.text("${target.name}：", NamedTextColor.WHITE))
                .append(Component.text(if (tier == null) "无背包" else "背包 ${tier.id}", NamedTextColor.GRAY))
                .append(Component.text("，存储行 $rows", NamedTextColor.GRAY))
                .append(Component.text("，可用存储槽 ${rangeText(allowed)}", NamedTextColor.GRAY))
                .append(Component.text("，锁定槽 ${rangeText(locked)}", NamedTextColor.GRAY))
                .build(),
        )
    }

    private fun rangeText(range: IntRange): String = if (range.isEmpty()) "无" else range.toString()

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
