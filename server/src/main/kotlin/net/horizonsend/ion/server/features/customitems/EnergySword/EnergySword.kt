package net.horizonsend.ion.server.features.customitems.EnergySword

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration.EnergyWeapons.EnergySwordBalancing
import net.horizonsend.ion.server.features.customitems.CustomItem
import net.horizonsend.ion.server.features.nations.gui.item
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys.BLOCK
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys.TIMELASTUSED
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.world.item.ShieldItem
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.math.roundToInt

abstract class EnergySword<T : EnergySwordBalancing>(
	identifier: String,

	private val material: Material,
	internal val customModelData: Int,
	internal val displayName: Component,

	balancingSupplier: Supplier<T>
) : CustomItem(identifier) {
	val balancing = balancingSupplier.get()
	override fun handlePrimaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
		livingEntity.world.playSound(livingEntity.location, "energy_sword.swing", 1.5f, 1.0f)
	}

	override fun handleSecondaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
		val player = livingEntity as? Player ?: return
		if (player.isBlocking) {
			val currentBlock = getCurrentBlock(itemStack)
			player.sendActionBar(
				Component.text(
					"Block: $currentBlock / ${balancing.blockAmount}",
					NamedTextColor.GREEN,
					TextDecoration.BOLD,
					TextDecoration.UNDERLINED
				)
			)
			//Set Last Use
			itemStack.itemMeta.persistentDataContainer.set(
				TIMELASTUSED,
				PersistentDataType.LONG,
				System.currentTimeMillis()
			)
		}
	}

	override fun handleTertiaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
		if ((livingEntity as Player).hasCooldown(itemStack.type))return
		//Put this time as a tertiaryInteract, this is so we can track when people parry
		EnergySwordListener.peopleToLastParryTime[livingEntity as Player] = System.currentTimeMillis()
		livingEntity.setCooldown(itemStack.type, 20) //Add a cooldown, so players don't spam it
		livingEntity.sendActionBar(Component.text("Tried to parry!", TextColor.color(0, 0, 255)))
	}

	fun getCurrentBlock(itemStack: ItemStack): Double {
		//Find when the energySword was last used, so we can add block to it depended on how long ago it was, a recharge of sorts
		val lastUsed =
			itemStack.itemMeta.persistentDataContainer.get(TIMELASTUSED, PersistentDataType.LONG) ?: return 0.0
		val amountOfSecondsSinceLastUse =
			-(TimeUnit.MILLISECONDS.toSeconds(lastUsed) - (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())))
		val amountOfTicksSinceLastUse = amountOfSecondsSinceLastUse * 20
		val amountTheBlockShouldveRecharged = amountOfTicksSinceLastUse * balancing.blockRechargePerTick
		val currentBlock = itemStack.itemMeta.persistentDataContainer.get(BLOCK, PersistentDataType.DOUBLE)!!
		itemStack.editMeta {
			itemStack.itemMeta.persistentDataContainer.set(
				TIMELASTUSED,
				PersistentDataType.LONG,
				System.currentTimeMillis()
			)
			if (currentBlock != balancing.blockAmount) {
				it.persistentDataContainer.set(
					BLOCK,
					PersistentDataType.DOUBLE,
					it.persistentDataContainer.get(BLOCK, PersistentDataType.DOUBLE)!!
						.plus(amountTheBlockShouldveRecharged)
				)
			}
			if (currentBlock <0.0){
				it.persistentDataContainer.set(BLOCK, PersistentDataType.DOUBLE, 0.0)
			}
		}

		return (itemStack.itemMeta.persistentDataContainer.get(BLOCK, PersistentDataType.DOUBLE)?.coerceIn(0.0, 20.0))
			?: 0.0
	}

	fun setCurrentBlock(itemStack: ItemStack, block: Double, livingEntity: LivingEntity?) {
		//Show the block as durability
		itemStack.editMeta {
			//Set Last Use
			it.persistentDataContainer.set(TIMELASTUSED, PersistentDataType.LONG, System.currentTimeMillis())
			//Set Block
			it.persistentDataContainer.set(BLOCK, PersistentDataType.DOUBLE, block)
		}
		//If the Block ammount to set is 0, put a cooldown on the energy sword untill its recharged
		if (getCurrentBlock(itemStack) <= 0.0) {
			(livingEntity as? Player)?.setCooldown(
				itemStack.type,
				(balancing.blockAmount / balancing.blockRechargePerTick).toInt()
			)
			livingEntity?.clearActiveItem()
			itemStack.editMeta {
				it.persistentDataContainer.set(BLOCK, PersistentDataType.DOUBLE, 0.0)
			}
		}
		livingEntity?.sendActionBar(
			Component.text(
				"Block: $block / ${balancing.blockAmount}",
				NamedTextColor.GREEN,
				TextDecoration.BOLD,
				TextDecoration.UNDERLINED
			)
		)
	}
}
