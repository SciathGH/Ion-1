package net.horizonsend.ion.server.features.custom.items.util

import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.features.machine.PowerMachines
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.text.itemLore
import net.horizonsend.ion.server.miscellaneous.utils.updatePersistentDataContainer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.AQUA
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * A custom item that holds a value, e.g. ammo, power, gas, etc.
 **/
enum class StoredValues(
	val key: NamespacedKey
) {
	AMMO(NamespacedKeys.AMMO) {
		override fun formatLore(amount: Int, maxAmount: Int): Component = ofChildren(
			text("Ammo: ", GRAY),
			text(amount, AQUA),
			text(" / ", GRAY),
			text(maxAmount, AQUA)
		).itemLore
	},

	POWER(NamespacedKeys.POWER) {
		override fun formatLore(amount: Int, maxAmount: Int): Component = ofChildren(
			PowerMachines.prefixComponent,
			text(amount, GREEN),
			text(" / ", GRAY),
			text(maxAmount, YELLOW)
		).itemLore
	},

	GAS(NamespacedKeys.GAS) {
		override fun formatLore(amount: Int, maxAmount: Int): Component = ofChildren(
			text("Gas: ", GRAY),
			text(amount, AQUA),
			text(" / ", GRAY),
			text(maxAmount, AQUA)
		).itemLore
	},

	ENERGYSWORDBLOCK(NamespacedKeys.ENERGY_BLOCK_AMOUNT) {
		override fun formatLore(amount: Int, maxAmount: Int): Component = ofChildren(
			text("Block: ", GRAY),
			text(amount, AQUA),
			text(" / ", GRAY),
			text(maxAmount, AQUA)
		).itemLore
	},

	TIMELASTUSED(NamespacedKeys.TIME_LAST_USED) {
		override fun formatLore(amount: Int, maxAmount: Int): Component = ofChildren(
			text("TimeLastUsed: ", GRAY),
			text(amount, GRAY),
		).itemLore
	}

	;

	abstract fun formatLore(amount: Int, maxAmount: Int): Component

	fun getAmount(itemStack: ItemStack): Int {
		return itemStack.itemMeta.persistentDataContainer.getOrDefault(key, PersistentDataType.INTEGER, 0)
	}

	fun setAmount(itemStack: ItemStack, amount: Int) = itemStack.updatePersistentDataContainer { set(key, PersistentDataType.INTEGER, amount) }
}
