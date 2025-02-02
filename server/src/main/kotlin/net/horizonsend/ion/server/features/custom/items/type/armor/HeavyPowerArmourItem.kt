@file:Suppress("UnstableApiUsage")

package net.horizonsend.ion.server.features.custom.items.type.armor

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.Unbreakable
import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes
import net.horizonsend.ion.server.features.custom.items.component.CustomItemComponentManager
import net.horizonsend.ion.server.features.custom.items.component.ModManager
import net.horizonsend.ion.server.features.custom.items.component.PowerStorage
import net.horizonsend.ion.server.features.custom.items.component.TickReceiverModule
import net.horizonsend.ion.server.features.custom.items.util.ItemFactory
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class HeavyPowerArmourItem(identifier: String, displayName: Component, itemModel: String, slot: EquipmentSlot, maxPrimaryMods: Int, maxSecondaryMods: Int, balancingSupplier: PVPBalancingConfiguration.Armour.AttributeHolder): PowerArmorItem(
	identifier, displayName,
	ItemFactory
		.builder()
		.setMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
		.setCustomModel(itemModel)
		.setMaxStackSize(1)
		.addData(DataComponentTypes.UNBREAKABLE, Unbreakable.unbreakable(false))
		.addData(
			DataComponentTypes.EQUIPPABLE, Equippable
			.equippable(slot)
			.damageOnHurt(false)
			.swappable(true)
			.equipSound(Key.key("minecraft", "item.armor.equip_netherite"))
			.assetId(NamespacedKeys.packKey("power_armor"))
			.build()
		)
		.build(),
	slot,
	maxPrimaryMods,
	maxSecondaryMods,
	balancingSupplier
) {
	override val customComponents: CustomItemComponentManager = super.customComponents.apply {
		addComponent(CustomComponentTypes.POWER_STORAGE, PowerStorage(100000, 0, true))
		addComponent(CustomComponentTypes.MOD_MANAGER, ModManager(maxPrimaryMods = 1, maxSecondaryMods = 2 ))
		addComponent(CustomComponentTypes.TICK_RECIEVER, TickReceiverModule(10) { entity, itemStack, _, _ -> handleHeavyArmourEnvironmentalEffects(entity, itemStack)},
		)
	}

	private fun handleHeavyArmourEnvironmentalEffects(entity: LivingEntity, itemStack: ItemStack){

	}

}
