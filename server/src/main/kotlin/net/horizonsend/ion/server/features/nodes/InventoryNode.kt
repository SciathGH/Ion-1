package net.horizonsend.ion.server.features.nodes

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class InventoryNode : AbstractNode() {
	abstract val inventory: Inventory
	open fun input(item: ItemStack){
		if(inventory.size == inventory.maxStackSize)return
		inventory.addItem(item) //todo: dont do it this way
	}
}
