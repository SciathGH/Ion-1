package net.horizonsend.ion.server.features.custom.items.type.tool.mods.armor.secondary

import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.custom.items.attribute.CustomItemAttribute
import net.horizonsend.ion.server.features.custom.items.type.armor.PowerArmorItem
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ApplicationPredicate
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ItemModification
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ModificationItem
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.function.Supplier
import kotlin.reflect.KClass

object ArmourBoostMod : ItemModification {
	override val applicationPredicates: Array<ApplicationPredicate> = arrayOf(ApplicationPredicate.ClassPredicate(PowerArmorItem::class))
	override val incompatibleWithMods: Array<KClass<out ItemModification>> = arrayOf()
	override val modItem: Supplier<ModificationItem?> = Supplier { CustomItemRegistry.ARMOR_MODIFICATION_ARMOR_BOOST }
	override val crouchingDisables: Boolean = false
	override val identifier: String = "ARMOUR_BOOST"
	override val displayName: Component = ofChildren(Component.text("Armour Boost", NamedTextColor.GRAY),
		Component.text(" Module, ", NamedTextColor.GOLD),
		Component.text("Secondary Module", NamedTextColor.GRAY)
	)
	override val primaryOrSecondary: ItemModification.PrimaryOrSecondary = ItemModification.PrimaryOrSecondary.SECONDARY
	private val namedSpaceKey = NamespacedKeys.key(identifier)
	val armor = Pair(Attribute.ARMOR, AttributeModifier(namedSpaceKey, 0.5, AttributeModifier.Operation.ADD_SCALAR))
	val movementSpeed = Pair(Attribute.MOVEMENT_SPEED,AttributeModifier(namedSpaceKey, -0.05, AttributeModifier.Operation.ADD_SCALAR))
	override fun onAdd(itemStack: ItemStack) {
			itemStack.editMeta {
				it.addAttributeModifier(
					armor.first,
					armor.second
				)
				it.addAttributeModifier(
					movementSpeed.first,
					movementSpeed.second
				)
		}
	}

	override fun onRemove(itemStack: ItemStack) {
		itemStack.editMeta {
			it.removeAttributeModifier(
				armor.first,
				armor.second
			)
			it.removeAttributeModifier(
				movementSpeed.first,
				movementSpeed.second
			)
		}
	}
	override fun getAttributes(): List<CustomItemAttribute> = listOf()
}
