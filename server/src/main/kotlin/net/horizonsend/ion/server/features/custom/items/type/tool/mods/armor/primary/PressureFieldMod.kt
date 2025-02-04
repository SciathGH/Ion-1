package net.horizonsend.ion.server.features.custom.items.type.tool.mods.armor.primary

import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.custom.items.attribute.CustomItemAttribute
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ApplicationPredicate
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ItemModification
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ModificationItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import org.bukkit.inventory.EquipmentSlot
import java.util.function.Supplier
import kotlin.reflect.KClass

object PressureFieldMod : ItemModification {
	override val applicationPredicates: Array<ApplicationPredicate> = arrayOf(ApplicationPredicate.EquipmentSlotPredicate(
		EquipmentSlot.HEAD))
	override val incompatibleWithMods: Array<KClass<out ItemModification>> = arrayOf(EnvironmentMod::class, EnvironmentMod::class)
	override val modItem: Supplier<ModificationItem?> = Supplier { CustomItemRegistry.ARMOR_MODIFICATION_PRESSURE_FIELD }
	override val crouchingDisables: Boolean = false
	override val identifier: String = "PRESSURE_FIELD"
	override val displayName: Component = ofChildren(Component.text("Pressure Field", GRAY), Component.text(" Module", GOLD),Component.text("\n Primary Module",NamedTextColor.DARK_GRAY))
	override val primaryOrSecondary: ItemModification.PrimaryOrSecondary = ItemModification.PrimaryOrSecondary.PRIMARY

	override fun getAttributes(): List<CustomItemAttribute> = listOf()
}
