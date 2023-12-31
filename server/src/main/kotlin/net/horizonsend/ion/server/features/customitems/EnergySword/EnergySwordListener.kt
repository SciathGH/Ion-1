package net.horizonsend.ion.server.features.customitems.EnergySword

import net.horizonsend.ion.common.database.schema.nations.Nation
import net.horizonsend.ion.common.utils.miscellaneous.randomInt
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.cache.PlayerCache
import net.horizonsend.ion.server.features.customitems.CustomItems.customItem
import net.horizonsend.ion.server.features.customitems.blasters.objects.Blaster
import net.horizonsend.ion.server.listener.SLEventListener
import net.horizonsend.ion.server.miscellaneous.utils.Notify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.util.TimeUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class EnergySwordListener : SLEventListener() {

	@Suppress("Unused")
	@EventHandler
	fun onDeath(event: PlayerDeathEvent){
		val victim = event.player
		val killer = event.entity.killer ?: return
		val customItem = killer.inventory.itemInMainHand.customItem ?: return

		if (customItem !is EnergySword<*>) return

		val arena: String = if (IonServer.configuration.serverName.equals("creative", ignoreCase = true))
			"<#555555>[<#ffff66>Arena<#555555>]<reset> "
		else ""

		val verb = when(randomInt(0, 33)){
			0-> "cut down"
			1-> "kebabed"
			2-> "stabbed"
			3-> "cooked"
			4-> "mauled"
			5-> "slain"
			6-> "pierced"
			7-> "slashed"
			8-> "thrusted"
			9-> "poked"
			10-> "deep fried"
			11-> "split open"
			12-> "cleaved"
			13-> "discombobulated"
			14-> "bamboozled"
			15-> "put straight"
			16-> "diced"
			17-> "skewered"
			18-> "gooped"
			19-> "whacked"
			20-> "beheaded"
			21-> "executed"
			22-> "knocked out"
			23-> "killed"
			24-> "butchered"
			25-> "carved"
			26-> "massacred"
			27-> "dispatched"
			28-> "gutted"
			29-> "destroyed"
			30-> "eliminated"
			31-> "smoked"
			32-> "neutralised"
			33-> "sliced in half"
			else -> "deezed" //should never happen
		}

		val name = customItem.displayName
		val victimColor =
			"<#" + Integer.toHexString((
				PlayerCache[victim].nationOid?.let { Nation.findById(it) }?.color
					?: 16777215
				)) + ">"

		val killerColor =
			"<#" + Integer.toHexString((
				PlayerCache[killer].nationOid?.let { Nation.findById(it) }?.color
					?: 16777215
				)) + ">"

		val newMessage = MiniMessage.miniMessage()
			.deserialize(
				"$arena$victimColor${victim.name}<reset> was $verb by $killerColor${killer.name}<reset> using "
			)
			.append(name)

		if (victim.world.name.contains("arena", ignoreCase = true)) {
			event.deathMessage(newMessage)
		} else {
			event.deathMessage(null)

			if (IonServer.configuration.serverName == "survival") Notify.online(newMessage) else IonServer.server.sendMessage(newMessage)
		}
	}

	@EventHandler
	fun onPlayerItemHoldEvent(event: PlayerItemHeldEvent) {
		val itemStack = event.player.inventory.getItem(event.player.inventory.heldItemSlot) ?: return
		val customItem = itemStack.customItem as? EnergySword<*> ?: return

		val block = customItem.getCurrentBlock(itemStack)

		event.player.sendActionBar(
			Component.text(
				"block: $block / ${customItem.balancing.blockAmount}",
				NamedTextColor.GREEN
			)
		)
	}
}
