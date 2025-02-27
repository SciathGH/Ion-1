package net.horizonsend.ion.server.features.custom.items.type.tool.mods.tool.hoe

import net.horizonsend.ion.common.utils.text.ITALIC
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.custom.items.attribute.CustomItemAttribute
import net.horizonsend.ion.server.features.custom.items.type.tool.PowerHoe
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ApplicationPredicate
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ItemModification
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ModificationItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Supplier
import kotlin.reflect.KClass

object FertilizerDispenser : ItemModification {
	override val identifier: String = "FERTILIZER_DISPENSER"
	override val displayName: Component = text("Fertilizer Sprayer", DARK_GREEN).decoration(ITALIC, false)

	override val applicationPredicates: Array<ApplicationPredicate> = arrayOf(ApplicationPredicate.ClassPredicate(PowerHoe::class))
	override val incompatibleWithMods: Array<KClass<out ItemModification>> = arrayOf()

	override val modItem: Supplier<ModificationItem?> = Supplier { CustomItemRegistry.FERTILIZER_DISPENSER }

	override val crouchingDisables: Boolean = true
	override val primaryOrSecondary: ItemModification.PrimaryOrSecondary = ItemModification.PrimaryOrSecondary.PRIMARY

	fun fertilizeCrop(player: Player, block: Block): Boolean {
		if (!player.inventory.contains(Material.BONE_MEAL)) return false

		val result = block.applyBoneMeal(BlockFace.DOWN)

		if (result) {
			player.inventory.removeItemAnySlot(ItemStack(Material.BONE_MEAL))
		}

		return result
	}

	override fun getAttributes(): List<CustomItemAttribute> = listOf()
}
