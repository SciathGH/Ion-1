package net.horizonsend.ion.server.features.customitems.powerArmour

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.customitems.CustomItem
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.function.Supplier

abstract class PowerArmour <T: PVPBalancingConfiguration.Armours.PowerArmourBalancing> (
	identifier: String,

	balancingSupplier: Supplier<T>
): CustomItem(identifier) {
	//On Hit
	override fun handlePrimaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {

	}

	//Module's, should be called every tick, refreshes which modules the player has on them
	override fun handleSecondaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {

	}

	//TODO: Add in a New customItem type called "PoweredCustomitem"
	fun getPower(itemStack: ItemStack) : Double{
		return itemStack.itemMeta.persistentDataContainer.get(NamespacedKeys.GEARPOWER, PersistentDataType.DOUBLE) ?: 0.0
	}
	fun setPower(itemStack: ItemStack, power: Double){
		itemStack.editMeta { it.persistentDataContainer.set(NamespacedKeys.GEARPOWER, PersistentDataType.DOUBLE, power) }
	}


	fun removePower(itemStack: ItemStack, powerToRemove: Double){
		val newPower = (itemStack.itemMeta.persistentDataContainer.get(NamespacedKeys.GEARPOWER, PersistentDataType.DOUBLE) ?: 0.0).minus(powerToRemove)
		itemStack.editMeta { it.persistentDataContainer.set(NamespacedKeys.GEARPOWER, PersistentDataType.DOUBLE, newPower) }
	}
}
