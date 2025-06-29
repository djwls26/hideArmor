package me.leeyu.hideArmor

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import com.comphenix.protocol.wrappers.Pair as ProtocolPair
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

object HideManager {
    private val hiddenPlayers = mutableSetOf<UUID>()
    var permission: String = "armorhide.use"

    fun reload() {
        permission = HideArmor.instance.config.getString("permission") ?: "armorhide.use"
    }

    fun toggle(player: Player): Boolean {
        val uuid = player.uniqueId
        return if (hiddenPlayers.remove(uuid)) {
            saveHiddenPlayers()
            sendArmorUpdate(player)
            false
        } else {
            hiddenPlayers.add(uuid)
            saveHiddenPlayers()
            sendArmorUpdate(player)
            true
        }
    }

    fun clear() {
        hiddenPlayers.clear()
    }

    fun saveHiddenPlayers() {
        val config = HideArmor.instance.dataConfig
        config.set("hiddenPlayers", hiddenPlayers.map { it.toString() })
        HideArmor.instance.saveDataConfig()
    }

    fun loadHiddenPlayers() {
        val config = HideArmor.instance.dataConfig
        val list = config.getStringList("hiddenPlayers")
        hiddenPlayers.clear()
        hiddenPlayers.addAll(list.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() })
    }

    fun register() {
        val protocolManager = ProtocolLibrary.getProtocolManager()

        protocolManager.addPacketListener(object :
            PacketAdapter(HideArmor.instance, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            override fun onPacketSending(event: PacketEvent) {
                val viewer = event.player
                val entityId = event.packet.integers.read(0)
                val target = Bukkit.getOnlinePlayers().find { it.entityId == entityId } ?: return

                if (!hiddenPlayers.contains(target.uniqueId)) return

                val original = event.packet.slotStackPairLists.read(0)
                val modified = original.map { pair ->
                    val slot = pair.first
                    val item = pair.second

                    if (slot in listOf(ItemSlot.CHEST, ItemSlot.LEGS, ItemSlot.FEET)) {
                        val disguised = item.clone()
                        disguised.type = Material.BARRIER

                        val originalMeta = item.itemMeta
                        val newMeta = disguised.itemMeta

                        if (originalMeta != null && newMeta != null) {
                            newMeta.setDisplayName(
                                if (originalMeta.hasDisplayName()) {
                                    originalMeta.displayName!!
                                } else {
                                    "§a갑옷 숨겨짐"
                                }
                            )
                            newMeta.lore = originalMeta.lore

                            originalMeta.enchants.forEach { (enchant, level) ->
                                newMeta.addEnchant(enchant, level, true)
                            }

                            newMeta.attributeModifiers = originalMeta.attributeModifiers

                            disguised.itemMeta = newMeta
                        }

                        ProtocolPair(slot, disguised)
                    } else {
                        ProtocolPair(slot, item)
                    }
                }
                val clone = event.packet.shallowClone()
                clone.slotStackPairLists.write(0, modified)
                event.packet = clone
            }
        })

        Bukkit.getScheduler().runTaskTimer(HideArmor.instance, Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                if (hiddenPlayers.contains(player.uniqueId)) {
                    sendArmorUpdate(player)
                }
            }
        }, 0L, 2L)
    }

    private fun sendArmorUpdate(player: Player) {
        val protocolManager = ProtocolLibrary.getProtocolManager()
        val isHidden = hiddenPlayers.contains(player.uniqueId)

        val equipment = listOf(
            ItemSlot.HEAD to player.inventory.helmet,
            ItemSlot.CHEST to player.inventory.chestplate,
            ItemSlot.LEGS to player.inventory.leggings,
            ItemSlot.FEET to player.inventory.boots
        )

        Bukkit.getOnlinePlayers().forEach { viewer ->
            val shouldHide = isHidden && viewer.uniqueId != player.uniqueId

            val protocolPairs = equipment.map { (slot, item) ->
                val visibleItem = item ?: ItemStack(Material.AIR)
                val finalItem = if (
                    shouldHide && slot in listOf(ItemSlot.CHEST, ItemSlot.LEGS, ItemSlot.FEET)
                ) {
                    visibleItem.clone().apply { type = Material.BARRIER }
                } else {
                    visibleItem
                }
                ProtocolPair(slot, finalItem)
            }

            val packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
            packet.integers.write(0, player.entityId)
            packet.slotStackPairLists.write(0, protocolPairs)

            protocolManager.sendServerPacket(viewer, packet)
        }
    }
}