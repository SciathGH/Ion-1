package net.horizonsend.ion.server.features.customitems.EnergySword

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration.EnergyWeapons.EnergySwordBalancing
import net.horizonsend.ion.server.features.customitems.CustomItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

abstract class EnergySword <T: EnergySwordBalancing>(
	identifier: String,

	material: Material,
	customModelData: Int,
	displayName: Component
): CustomItem(identifier) {
	override fun handlePrimaryInteract(livingEntity: LivingEntity, itemStack: ItemStack) {
	}

}
