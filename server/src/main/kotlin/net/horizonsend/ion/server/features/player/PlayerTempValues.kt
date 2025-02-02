package net.horizonsend.ion.server.features.player

import net.horizonsend.ion.server.listener.SLEventListener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.net.http.WebSocket.Listener
import java.util.UUID

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

class PlayerTempValues(
	var shouldDoubleJump: Boolean = true,
	var canRocketFly: Boolean = false
	)

