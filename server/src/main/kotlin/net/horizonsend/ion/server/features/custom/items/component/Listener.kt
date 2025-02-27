package net.horizonsend.ion.server.features.custom.items.component

import io.papermc.paper.event.block.BlockPreDispenseEvent
import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.attribute.CustomItemAttribute
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KClass

class Listener<E: Event, T: CustomItem>(
	val customItem: T,
	val eventType: KClass<out E>,
	private val preCheck: (E, T, ItemStack) -> Boolean = { _, _, _ -> true },
	private val eventReceiver: (E, T, ItemStack) -> Unit
) : CustomItemComponent {

	fun handleEvent(event: E, itemStack: ItemStack) = eventReceiver.invoke(event, customItem, itemStack)
	fun preCheck(event: E, itemStack: ItemStack) = preCheck.invoke(event, customItem, itemStack)

	override fun decorateBase(baseItem: ItemStack, customItem: CustomItem) {}
	override fun getAttributes(baseItem: ItemStack): Iterable<CustomItemAttribute> = listOf()

	companion object {
		/**
		 * General interact listener
		 **/
		inline fun <reified T: CustomItem> interactListener(
			customItem: T,
			noinline handleEvent: (PlayerInteractEvent, T, ItemStack) -> Unit
		): Listener<PlayerInteractEvent, T> = Listener(customItem, PlayerInteractEvent::class, eventReceiver = handleEvent)

		/**
		 * Interact listener filtered for right clicks
		 **/
		inline fun <reified T: CustomItem> rightClickListener(
			customItem: T,
			crossinline additionalPreCheck: (PlayerInteractEvent) -> Boolean = { true },
			noinline handleEvent: (PlayerInteractEvent, T, ItemStack) -> Unit
		): Listener<PlayerInteractEvent, T> = Listener(
			customItem,
			PlayerInteractEvent::class,
			preCheck = { event, _, _ -> (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) && additionalPreCheck.invoke(event) },
			handleEvent
		)

		/**
		 * General interact listener filtered for left clicks
		 **/
		inline fun <reified T: CustomItem> leftClickListener(
			customItem: T,
			crossinline additionalPreCheck: (PlayerInteractEvent) -> Boolean = { true },
			noinline handleEvent: (PlayerInteractEvent, T, ItemStack) -> Unit
		): Listener<PlayerInteractEvent, T> = Listener(
			customItem,
			PlayerInteractEvent::class,
			preCheck = { event, _, _ -> (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) && additionalPreCheck.invoke(event) },
			handleEvent
		)

		inline fun <reified T: CustomItem> playerSwapHandsListener(
			customItem: T,
			noinline handleEvent: (PlayerSwapHandItemsEvent, T, ItemStack) -> Unit
		): Listener<PlayerSwapHandItemsEvent, T> = Listener(customItem, PlayerSwapHandItemsEvent::class, eventReceiver = handleEvent)

		inline fun <reified T: CustomItem> dispenseListener(
			customItem: T,
			noinline handleEvent: (BlockPreDispenseEvent, T, ItemStack) -> Unit
		): Listener<BlockPreDispenseEvent, T> = Listener(customItem, BlockPreDispenseEvent::class, eventReceiver = handleEvent)

		inline fun <reified T: CustomItem> entityShootBowListener(
			customItem: T,
			noinline handleEvent: (EntityShootBowEvent, T, ItemStack) -> Unit
		): Listener<EntityShootBowEvent, T> = Listener(customItem, EntityShootBowEvent::class, eventReceiver =  handleEvent)

		inline fun <reified T: CustomItem> prepareCraftListener(
			customItem: T,
			noinline handleEvent: (PrepareItemCraftEvent, T, ItemStack) -> Unit
		): Listener<PrepareItemCraftEvent, T> = Listener(customItem, PrepareItemCraftEvent::class, eventReceiver =  handleEvent)

		inline fun <reified T: CustomItem> damageEntityListener(
			customItem: T,
			noinline handleEvent: (EntityDamageByEntityEvent, T, ItemStack) -> Unit
		): Listener<EntityDamageByEntityEvent, T> = Listener(
			customItem,
			EntityDamageByEntityEvent::class,
			preCheck = { event, customItem, itemStack ->
				val damager = event.damager as? LivingEntity ?: return@Listener false
				val itemInHand = damager.equipment?.itemInMainHand ?: return@Listener false
				itemInHand.customItem == customItem
			},
			eventReceiver =  handleEvent
		)

		inline fun <reified T: CustomItem> damagedHoldingListener(
			customItem: T,
			noinline handleEvent: (EntityDamageByEntityEvent, T, ItemStack) -> Unit
		): Listener<EntityDamageByEntityEvent, T> = Listener(
			customItem,
			EntityDamageByEntityEvent::class,
			preCheck = { event, customItem, itemStack ->
				val damaged = event.entity as? LivingEntity ?: return@Listener false

				val itemInMainHand = damaged.equipment?.itemInMainHand
				val mainHandcustomItem = itemInMainHand?.customItem

				val itemInOffHand = damaged.equipment?.itemInOffHand
				val offHandcustomItem = itemInOffHand?.customItem

				(itemInOffHand != null && offHandcustomItem != null) || (itemInMainHand != null && mainHandcustomItem != null)
			},
			eventReceiver =  handleEvent
		)

		inline fun <reified T: CustomItem> holdingListener(
			customItem: T,
			noinline handleEvent: (PlayerItemHeldEvent, T, ItemStack) -> Unit
		): Listener<PlayerItemHeldEvent, T> = Listener(
			customItem,
			PlayerItemHeldEvent::class,
			preCheck = { event, customItem, itemStack ->
				val itemInOffHand = event.player.inventory.itemInOffHand
				val offHandcustomItem = itemInOffHand.customItem

				val itemInMainHand = event.player.inventory.itemInMainHand
				val mainHandcustomItem = itemInMainHand.customItem
				(itemInOffHand != null && offHandcustomItem != null) || (itemInMainHand != null && mainHandcustomItem != null)
			},
			eventReceiver =  handleEvent
		)
	}
}
