package net.horizonsend.ion.server.features.customitems.energySword

import net.horizonsend.ion.common.database.schema.nations.Nation
import net.horizonsend.ion.common.utils.miscellaneous.randomInt
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.cache.PlayerCache
import net.horizonsend.ion.server.features.customitems.CustomItems.customItem
import net.horizonsend.ion.server.listener.SLEventListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemHeldEvent

object EnergySwordListener : SLEventListener() {
	val peopleToLastParryTime = mutableMapOf<Player, Long>() //Player to time

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

		val verb = when(randomInt(0, 34)){
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
			34-> "caught lacking"
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

		event.deathMessage(newMessage)
	}
	@EventHandler
	fun onPlayerItemHoldEvent(event: PlayerItemHeldEvent) {
		val itemStack =
			if (event.player.inventory.getItem(event.newSlot)?.type == Material.AIR) event.player.inventory.itemInOffHand
			else event.player.inventory.itemInMainHand
		val customItem = itemStack.customItem as? EnergySword<*> ?: return

		val block = customItem.getCurrentBlock(itemStack)

		event.player.sendActionBar(
			Component.text(
				"Block: $block / ${customItem.balancing.blockAmount}",
				NamedTextColor.GREEN,
				TextDecoration.BOLD,
				TextDecoration.UNDERLINED
			)
		)
	}

	@Suppress("DEPRECATION") //Deprecate because EntityDamageEvent.DamageModifier is deprecated despite there being no alternative
	@EventHandler(priority = EventPriority.LOW)
	fun onShieldHit(event: EntityDamageByEntityEvent){
		val item = (event.entity as? Player)?.activeItem //Get the item in use
		val customItem = item?.customItem ?: return //Get the customitem in use
 		if (customItem !is EnergySword<*>) return //Check if it's an energysword
		val currentBlock = customItem.getCurrentBlock(item) //Get current Block
		val damagedBlocked = -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) //Get the damage that a shield absorbed in the event
		if (currentBlock <= 0.0) {
			//If the current block is broken <=0 then set the cooldown, and importantly clear the active item so the player stops using the shield, to get infinite block
			(event.entity as? Player)?.setCooldown(item.type, (customItem.balancing.blockAmount/customItem.balancing.blockRechargePerTick).toInt())
			(event.entity as LivingEntity).clearActiveItem()
			return
		}
		// If the block isint zero then minus the damageBlocked from the currentBlock
		customItem.setCurrentBlock(item, currentBlock-damagedBlocked, event.entity as? LivingEntity)
		//If the currentBlock couldnt absorb all the damage that was absorbed by the shield during the event, damage the player with the excess damage
		val damageThatShouldHaveBeenDealt = damagedBlocked-currentBlock
		if (damageThatShouldHaveBeenDealt < 0.0) return
		event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, damagedBlocked-currentBlock)
		if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) <=0.0) event.isCancelled = true
	}
}
