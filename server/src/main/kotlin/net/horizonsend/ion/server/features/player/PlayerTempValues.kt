package net.horizonsend.ion.server.features.player

import net.horizonsend.ion.server.listener.SLEventListener
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

object PlayerTempValueManager : SLEventListener(){
	val UUIDToPlayerTempValue = mutableMapOf<UUID, PlayerTempValues>()

	@EventHandler
	fun playerJoinListener(event: PlayerJoinEvent){
		UUIDToPlayerTempValue.put(event.player.uniqueId, PlayerTempValues())
	}

	@EventHandler
	fun playerLeaveListener(event: PlayerQuitEvent){
		UUIDToPlayerTempValue.remove(event.player.uniqueId)
	}
 }

class PlayerTempValues{
	var maxRocketSpeed: Double = 1.0
	var rocketBootAccel: Double =  0.2
}

fun LivingEntity.getTempValues(): PlayerTempValues = PlayerTempValueManager.UUIDToPlayerTempValue[this.uniqueId] ?: PlayerTempValues()
