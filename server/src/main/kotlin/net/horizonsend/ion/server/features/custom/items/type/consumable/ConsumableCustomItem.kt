package net.horizonsend.ion.server.features.custom.items.type.consumable

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.util.ItemFactory
import net.kyori.adventure.text.Component
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import java.util.function.Supplier

abstract class ConsumableCustomItem(
	identifier: String,
	displayName: Component,
	baseItemFactory: ItemFactory,

	internal val balancingSupplier: Supplier<PVPBalancingConfiguration.Consumables.ConsumableBalancing>

) : CustomItem(identifier, displayName, baseItemFactory,) {

	open fun consume(itemStack: ItemStack, livingEntity: LivingEntity){}
}
