package me.leeyu.hideArmor

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§c이 명령어는 관리자만 사용할 수 있습니다.")
            return true
        }

        HideArmor.instance.reloadConfig()
        HideManager.reload()
        sender.sendMessage("§a[갑옷숨기기] 구성파일을 다시 불러왔습니다.")
        return true
    }
}