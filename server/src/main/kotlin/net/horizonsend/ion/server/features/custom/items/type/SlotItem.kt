package net.horizonsend.ion.server.features.custom.items.type

import org.bukkit.inventory.EquipmentSlot

/**
 * Slot item
 *
 * @constructor Create empty Slot item
 *
 * SlotItem is to be used for a customItem who's function is dependent on the slot its used in. Take for example Power
 * Armour and the fact we have a power armour helmet, chestplate, boots and leggings. This is useless for Mods which are
 * to only be applied to say a Power Armour helmet, like the pressure field module.
 */
 interface SlotItem {
	 val slot: EquipmentSlot
 }
