package me.leeyu.hideArmor

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class HideArmor : JavaPlugin() {

    companion object {
        lateinit var instance: HideArmor
            private set
    }

    private lateinit var dataFile: File
    lateinit var dataConfig: FileConfiguration
        private set

    override fun onEnable() {
        instance = this

        if (server.pluginManager.getPlugin("ProtocolLib") == null) {
            logger.severe("===============================================")
            logger.severe("ProtocolLib 플러그인이 설치되어 있지 않습니다.")
            logger.severe("hideArmor 플러그인을 비활성화합니다.")
            logger.severe("https://www.spigotmc.org/resources/protocollib.1997/")
            logger.severe("===============================================")
            server.pluginManager.disablePlugin(this)
            return
        }

        saveDefaultConfig()
        loadDataConfig()
        HideManager.reload()
        HideManager.loadHiddenPlayers()

        getCommand("갑옷숨기기")?.setExecutor(HideCommand())
        getCommand("갑옷숨기기리로드")?.setExecutor(ReloadCommand())

        HideManager.register()
        logger.info("hideArmor 플러그인이 정상적으로 활성화되었습니다.")
    }

    override fun onDisable() {
        saveDataConfig()
        HideManager.clear()
        logger.info("hideArmor 플러그인이 비활성화되었습니다.")
    }

    fun loadDataConfig() {
        dataFile = File(dataFolder, "data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            saveResource("data.yml", false)
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
    }

    fun saveDataConfig() {
        dataConfig.save(dataFile)
    }
}
