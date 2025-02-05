package net.horizonsend.ion.server.features.custom.items.type.consumable

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes
import net.horizonsend.ion.server.features.custom.items.component.CustomItemComponentManager
import net.horizonsend.ion.server.features.custom.items.component.Listener
import net.horizonsend.ion.server.features.custom.items.util.ItemFactory
import net.horizonsend.ion.server.features.nations.gui.item
import net.kyori.adventure.text.Component
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.function.Supplier

@Suppress("DEPRECATION")
class HealthStim(identifier: String, displayName: Component, baseItemFactory: ItemFactory,
				 balancingSupplier: Supplier<PVPBalancingConfiguration.Consumables.ConsumableBalancing>
) : ConsumableCustomItem(identifier, displayName,
	baseItemFactory,
	balancingSupplier
) {
	override fun consume(itemStack: ItemStack, livingEntity: LivingEntity) {
		if (livingEntity.health == livingEntity.maxHealth) return//this is deprecated, but on god I cant find the alternative
		if ((livingEntity as Player).hasCooldown(itemStack)) return
		livingEntity.heal(balancingSupplier.get().modifierValue, EntityRegainHealthEvent.RegainReason.MAGIC)
		livingEntity.clearActiveItem()
		(livingEntity as? Player)?.setCooldown(itemStack.type, balancingSupplier.get().cooldownTicks)
		itemStack.amount -= 1 //technically uneeded, however if this item were to be stackable we'd want this, as such ive kept it in
		if (itemStack.amount == 0) {
			val inventory = (livingEntity as? InventoryHolder)?.inventory ?: return
			inventory.remove(itemStack)
		}
	}

	override val customComponents: CustomItemComponentManager = CustomItemComponentManager(serializationManager).apply {
		addComponent(CustomComponentTypes.LISTENER_PLAYER_INTERACT, Listener.rightClickListener(this@HealthStim){event, _, item ->
			consume(item, event.player)
		})
	}
}
