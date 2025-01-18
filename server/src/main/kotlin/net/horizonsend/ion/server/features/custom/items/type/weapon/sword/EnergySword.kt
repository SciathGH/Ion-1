package net.horizonsend.ion.server.features.custom.items.type.weapon.sword

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.configuration.ConfigurationFiles
import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.component.BlockAmountComponent
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes
import net.horizonsend.ion.server.features.custom.items.component.CustomItemComponentManager
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.damageEntityListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.damagedHoldingListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.holdingListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.leftClickListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.playerSwapHandsListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.prepareCraftListener
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.rightClickListener
import net.horizonsend.ion.server.features.custom.items.util.ItemFactory
import net.horizonsend.ion.server.features.custom.items.util.StoredValues
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Material.SHIELD
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class EnergySword(type: String, color: TextColor) : CustomItem(
	"ENERGY_SWORD_${type.uppercase()}",
	displayName = ofChildren(Component.text(type.lowercase().replaceFirstChar { it.uppercase() }, color), Component.text(" Energy Sword", YELLOW)),
	baseItemFactory = ItemFactory.builder()
		.setMaterial(SHIELD)
		.setCustomModel("weapon/energy_sword/${type.lowercase()}_energy_sword")
		.addData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers
			.itemAttributes()
			.addModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(NamespacedKeys.key("energy_sword_damage"), 7.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
			.addModifier(Attribute.ATTACK_SPEED, AttributeModifier(NamespacedKeys.key("energy_sword_speed"), -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
			.addModifier(Attribute.MOVEMENT_SPEED, AttributeModifier(NamespacedKeys.key("energy_sword_boost_speed"), .2, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.MAINHAND))
			.build())
		.build()
) {
	val balancing = ConfigurationFiles.pvpBalancing().energyWeapons::energySwordBalancing.get()

	val blockComponent = BlockAmountComponent(balancing)

	override fun decorateItemStack(base: ItemStack) {
		blockComponent.setBlock(base, balancing.blockAmount, null)
		StoredValues.TIMELASTUSED.setAmount(base, (System.currentTimeMillis()/1000).toInt())
	}

	override val customComponents: CustomItemComponentManager = CustomItemComponentManager(serializationManager).apply {
		addComponent(CustomComponentTypes.ENERGY_SWORD_BLOCK_AMOUNT, blockComponent)

		addComponent(CustomComponentTypes.LISTENER_PLAYER_INTERACT, leftClickListener(this@EnergySword) { event, _, item ->
			blockComponent.getBlock(item, event.player)
			event.player.world.playSound(event.player.location, "energy_sword.swing", 1.0f, 1.0f)
			if (event.action != Action.LEFT_CLICK_BLOCK) return@leftClickListener
			val player = event.player

			if (player.gameMode != GameMode.CREATIVE) return@leftClickListener

			event.isCancelled = true
		})

		addComponent(CustomComponentTypes.LISTENER_PLAYER_INTERACT, rightClickListener(this@EnergySword) { event, _, item ->
			event.isCancelled = false //Uncancel the event, the precheck unfortunately cancels it
			val player = event.player
			if (player.isBlocking) {
				val block = blockComponent.getBlock(item, player)
				blockComponent.setBlock(item, block-5, player)
			}
		})

		addComponent(CustomComponentTypes.LISTENER_HOLDING, holdingListener(this@EnergySword) { event, _, item ->
			event.isCancelled = false
			val player = event.player
			if (player.isBlocking) {
				val block = blockComponent.getBlock(item, player)
				blockComponent.setBlock(item, block-5, player)
			}
		})

		addComponent(CustomComponentTypes.LISTENER_PREPARE_CRAFT, prepareCraftListener(this@EnergySword) { event, customItem, item ->
			val permission = "gear.energysword." + customItem.identifier.lowercase().removePrefix("energy_sword_")
			if (!event.view.player.hasPermission(permission)) {
				event.view.player.userError("You can only craft yellow energy swords unless you donate for other colors!")
				event.inventory.result = null
			}
		})

		addComponent(CustomComponentTypes.LISTENER_DAMAGE_ENTITY, damageEntityListener(this@EnergySword) { event, customItem, item ->
			val damaged = event.entity
			damaged.world.playSound(Sound.sound(key("horizonsend:energy_sword.strike"), Sound.Source.PLAYER, 1.0f, 1.0f), damaged)
		})

		addComponent(CustomComponentTypes.LISTENER_PLAYER_SWAP_HANDS, playerSwapHandsListener(this@EnergySword) { event, customItem, item ->
			//A 'parry' rebounds an incoming projectile perfectly to where the player is looking
			val player = event.player
			if ((player).hasCooldown(item))return@playerSwapHandsListener
			if (player.hasCooldown(item)) return@playerSwapHandsListener
			peopleToParryTime[player] = System.currentTimeMillis()
			player.setCooldown(item.type, 20) //Add a cooldown, so players don't spam it
			player.sendActionBar(Component.text("Tried to parry!", TextColor.color(0, 0, 255)))
		})
	}
	companion object {
		val peopleToParryTime: MutableMap<Player, Long> = mutableMapOf()
	}
}

