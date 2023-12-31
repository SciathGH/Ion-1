package net.horizonsend.ion.server.features.customitems.EnergySword

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration.EnergyWeapons.EnergySwordBalancing
import net.horizonsend.ion.server.features.customitems.CustomItem
import net.horizonsend.ion.server.features.nations.gui.item
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys.BLOCK
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys.TIMELASTUSED
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.math.roundToInt

abstract class EnergySword <T: EnergySwordBalancing>(
	identifier: String,

	private val material: Material,
	internal val customModelData: Int,
	internal val displayName: Component,

	balancingSupplier: Supplier<T>
): CustomItem(identifier) {
	val balancing = balancingSupplier.get()
	override fun handlePrimaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
		livingEntity.world.playSound(livingEntity.location, "energy_sword.swing", 1.5f, 1.0f)
	}

	override fun handleSecondaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
		val player = livingEntity as? Player ?: return
		if (player.isBlocking){
			val currentBlock = getCurrentBlock(itemStack)
			player.sendActionBar(MiniMessage.miniMessage().deserialize("Current Block: $currentBlock / ${balancing.blockAmount}"))
			//Set Last Use
			itemStack.itemMeta.persistentDataContainer.set(TIMELASTUSED, PersistentDataType.LONG, System.currentTimeMillis())
		}
	}

	fun getCurrentBlock(itemStack: ItemStack) : Double {
		//Find when the energySword was last used, so we can add block to it depended on how long ago it was, a recharge of sorts
		val lastUsed = itemStack.itemMeta.persistentDataContainer.get(TIMELASTUSED, PersistentDataType.LONG) ?: System.currentTimeMillis()
		val amountOfSecondsSinceLastUse = (TimeUnit.MILLISECONDS.toSeconds(lastUsed)-(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())))
		val amountOfTicksSinceLastUse = amountOfSecondsSinceLastUse*20
		val amountTheBlockShouldveRecharged = amountOfTicksSinceLastUse*balancing.blockRechargePerTick
		return (itemStack.itemMeta.persistentDataContainer.get(BLOCK, PersistentDataType.DOUBLE)
			?.coerceIn(0.0, balancing.blockAmount.toDouble())?.plus(amountTheBlockShouldveRecharged)) ?: 0.0
	}

	fun setCurrentBlock(itemStack: ItemStack, block: Double, livingEntity: LivingEntity?){
		//Set Last Use
		itemStack.itemMeta.persistentDataContainer.set(TIMELASTUSED, PersistentDataType.LONG, System.currentTimeMillis())
		//Set Block
		itemStack.itemMeta.persistentDataContainer.set(BLOCK, PersistentDataType.DOUBLE, block)
		//Show the block as durability
		itemStack.editMeta {
			(it as Damageable).damage =
				(itemStack.type.maxDurability - getCurrentBlock(itemStack).toDouble() / balancing.blockAmount * itemStack.type.maxDurability).roundToInt()
		}
		//If the Block ammount to set is 0, put a cooldown on the energy sword untill its recharged
		(livingEntity as? Player)?.setCooldown(itemStack.type, (balancing.blockAmount/balancing.blockRechargePerTick).toInt())
	}
}
