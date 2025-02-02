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

object ShockAbsorbingMod : ItemModification {
	override val applicationPredicates: Array<ApplicationPredicate> = arrayOf(ApplicationPredicate.EquipmentSlotPredicate(
		EquipmentSlot.CHEST))
	override val incompatibleWithMods: Array<KClass<out ItemModification>> = arrayOf()
	override val modItem: Supplier<ModificationItem?> = Supplier { CustomItemRegistry.ARMOR_MODIFICATION_SHOCK_ABSORBING }
	override val crouchingDisables: Boolean = false
	override val identifier: String = "SHOCK_ABSORBING"
	override val displayName: Component = ofChildren(Component.text("Shock Absorbing", GRAY), Component.text(" Module", GOLD), Component.newline(), Component.text(" Primary Module", NamedTextColor.DARK_GRAY))
	override val primaryOrSecondary: ItemModification.PrimaryOrSecondary = ItemModification.PrimaryOrSecondary.PRIMARY

	override fun getAttributes(): List<CustomItemAttribute> = listOf()
}
