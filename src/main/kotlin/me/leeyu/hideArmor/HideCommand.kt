package me.leeyu.hideArmor

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HideCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player ?: return true

        if (!player.hasPermission(HideManager.permission)) {
            player.sendMessage("§c권한이 부족합니다.")
            return true
        }

        val toggled = HideManager.toggle(player)
        if (toggled) {
            player.sendMessage("§e갑옷이 숨겨졌습니다.")
        } else {
            player.sendMessage("§a다시 갑옷이 보이게 됩니다.")
        }

        return true
    }
}
