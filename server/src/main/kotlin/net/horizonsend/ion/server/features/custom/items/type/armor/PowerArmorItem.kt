package net.horizonsend.ion.server.features.custom.items.type.armor

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.item.Unbreakable
import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.attribute.PotionEffectAttribute
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes.Companion.MOD_MANAGER
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes.Companion.POWER_STORAGE
import net.horizonsend.ion.server.features.custom.items.component.CustomItemComponentManager
import net.horizonsend.ion.server.features.custom.items.component.Listener.Companion.rightClickListener
import net.horizonsend.ion.server.features.custom.items.component.ModManager
import net.horizonsend.ion.server.features.custom.items.component.PowerStorage
import net.horizonsend.ion.server.features.custom.items.component.TickReceiverModule
import net.horizonsend.ion.server.features.custom.items.type.SlotItem
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.ItemModRegistry
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.armor.primary.RocketBoostingMod
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.armor.primary.RocketBoostingMod.glideDisabledPlayers
import net.horizonsend.ion.server.features.custom.items.type.tool.mods.armor.primary.RocketBoostingMod.setGliding
import net.horizonsend.ion.server.features.custom.items.util.ItemFactory
import net.horizonsend.ion.server.features.starship.active.ActiveStarships
import net.horizonsend.ion.server.features.world.IonWorld.Companion.hasFlag
import net.horizonsend.ion.server.features.world.WorldFlag
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
class PowerArmorItem(
	identifier: String, displayName: Component, itemModel: String, override val slot: EquipmentSlot, val balancing: PVPBalancingConfiguration.Armor.AttributeHolder,
	baseItemFactory: ItemFactory = ItemFactory
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
) : CustomItem(identifier, displayName, baseItemFactory), SlotItem {


	//power armour specific components do not belong here, E.g not total power, or mods
	override val customComponents: CustomItemComponentManager = CustomItemComponentManager(serializationManager).apply {
		addComponent(CustomComponentTypes.LISTENER_PLAYER_INTERACT, rightClickListener(
			this@PowerArmorItem,
			additionalPreCheck = { it.player.isSneaking }
		) { event, _, item ->
			val modManger = getComponent(MOD_MANAGER)
			modManger.openMenu(event.player, this@PowerArmorItem, item)
		})

		//Put Tick Receiving Modules here
		val tickRecieverModule = mutableListOf(
			//interval in case it is not obvious is in ticks
			TickReceiverModule(20) { entity, itemStack, _, _ -> tickPowerMods(entity, itemStack)},
			TickReceiverModule(1) { entity, itemStack, _, equipmentSlot -> tickRocketBoots(entity, itemStack, equipmentSlot) },
			TickReceiverModule(45){ entity, itemStack, _, _ -> refreshModifiersFromBalancing(itemStack, entity)} //this does not need to be updated often
		)

		for(i in tickRecieverModule){
			addComponent(CustomComponentTypes.TICK_RECIEVER, i)
		}
		addComponent(MOD_MANAGER, ModManager(maxPrimaryMods = balancing.maxPrimaryModules, maxSecondaryMods = balancing.maxSecondaryModules ))
		addComponent(POWER_STORAGE, PowerStorage(balancing.power, 0, true))
	}

	override fun decorateItemStack(base: ItemStack) {
		base.editMeta { itemMeta ->
			attributeList().forEach {
				itemMeta.addAttributeModifier(it.key, it.value)
			}
			itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		}

	}

	fun attributeList() : MutableMap<Attribute, AttributeModifier> {
		return mutableMapOf(
			Attribute.MOVEMENT_SPEED to AttributeModifier(NamespacedKeys.key(identifier), balancing.speed , AttributeModifier.Operation.MULTIPLY_SCALAR_1, slot.group),
			Attribute.SNEAKING_SPEED to AttributeModifier(NamespacedKeys.key(identifier), balancing.sneakSpeed , AttributeModifier.Operation.MULTIPLY_SCALAR_1, slot.group),
			Attribute.SCALE to AttributeModifier(NamespacedKeys.key(identifier), balancing.scale , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.ENTITY_INTERACTION_RANGE to AttributeModifier(NamespacedKeys.key(identifier), balancing.entityReach , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.BLOCK_INTERACTION_RANGE to AttributeModifier(NamespacedKeys.key(identifier), balancing.blockReach , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.ARMOR to AttributeModifier(NamespacedKeys.key(identifier), balancing.armor , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.ARMOR_TOUGHNESS to AttributeModifier(NamespacedKeys.key(identifier), balancing.toughness, AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.KNOCKBACK_RESISTANCE to AttributeModifier(NamespacedKeys.key(identifier), balancing.knockBackResistance , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.STEP_HEIGHT to AttributeModifier(NamespacedKeys.key(identifier), balancing.stepHeight, AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.MAX_HEALTH to AttributeModifier(NamespacedKeys.key(identifier), balancing.maxHealth , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.JUMP_STRENGTH to AttributeModifier(NamespacedKeys.key(identifier), balancing.jumpStrength , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.FLYING_SPEED to AttributeModifier(NamespacedKeys.key(identifier), balancing.flyingSpeed , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.GRAVITY to AttributeModifier(NamespacedKeys.key(identifier), balancing.gravity , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.OXYGEN_BONUS to AttributeModifier(NamespacedKeys.key(identifier), balancing.oxygenBonus , AttributeModifier.Operation.ADD_NUMBER, slot.group),
			Attribute.WATER_MOVEMENT_EFFICIENCY to AttributeModifier(NamespacedKeys.key(identifier), balancing.waterMovementEfficiency , AttributeModifier.Operation.ADD_NUMBER, slot.group),
		)
	}

	fun refreshModifiersFromBalancing(itemStack: ItemStack, entity: LivingEntity) {
		val attributeList = attributeList()
		if (itemStack.itemMeta.attributeModifiers?.values()?.containsAll(attributeList.values) ==false){
			attributeList.forEach{
				itemStack.editMeta{itemMeta ->
					itemMeta.removeAttributeModifier(it.key, it.value)
					itemMeta.addAttributeModifier(it.key, it.value)
				}
			}
			entity.sendActionBar(Component.text("Updated PowerArmor with new Balancing Values"))
		}
		getComponent(POWER_STORAGE).setMaxPower(itemStack.customItem ?: return, itemStack, balancing.power)
	}

	fun tickPowerMods(entity: LivingEntity, itemStack: ItemStack) {
		val powerManager = getComponent(POWER_STORAGE)
		val power = powerManager.getPower(itemStack)
		if (power <= 0) return

		powerManager.removePower(itemStack, this, balancing.powerConsumedPerSecond) //consume base power

		val attributes = getAttributes(itemStack)
		for (attribute in attributes.filterIsInstance<PotionEffectAttribute>()) attribute.addPotionEffect(entity, this, itemStack)

		if (!getComponent(MOD_MANAGER).getPrimaryMods(itemStack).contains(ItemModRegistry.ROCKET_BOOSTING)) return
		if (entity !is Player) return
		if (entity.isGliding && !entity.world.hasFlag(WorldFlag.ARENA)) {
			powerManager.removePower(itemStack, this, 5)
		}
	}

	fun tickRocketBoots(entity: LivingEntity, itemStack: ItemStack, equipmentSlot: EquipmentSlot) {
		if (entity !is Player) return
		if (equipmentSlot != EquipmentSlot.FEET) return

		if (ActiveStarships.findByPilot(entity) != null && entity.inventory.itemInMainHand.type == Material.CLOCK) return

		val mods = getComponent(MOD_MANAGER).getAllMods(itemStack)
		if (!mods.contains(ItemModRegistry.ROCKET_BOOSTING)) {
			return setGliding(entity, false)
		}

		val powerManager = getComponent(POWER_STORAGE)
		if (powerManager.getPower(itemStack) <= 0) {
			return setGliding(entity, false)
		}

		if ((glideDisabledPlayers[entity.uniqueId] ?: 0) > System.currentTimeMillis()) return setGliding(entity, false)
		glideDisabledPlayers[entity.uniqueId]?.let { glideDisabledPlayers.remove(entity.uniqueId) } // remove if not disabled

		@Suppress("DEPRECATION") // Any other method would cause weirdness not allow low flight
		// RocketBoostingMod sets glidingPlayers only on the ToggleSneakEvent (in PowerArmorListener)
		if (entity.isOnGround || !entity.isSneaking || !RocketBoostingMod.glidingPlayers.contains(entity.uniqueId)) {
			setGliding(entity, false)
			return
		}

		entity.isGliding = true
		entity.velocity = entity.velocity.midpoint(entity.location.direction.multiply(0.6))
		entity.world.spawnParticle(Particle.SMOKE, entity.location, 5)

		if (!entity.world.hasFlag(WorldFlag.ARENA)) {
			powerManager.removePower(itemStack, this, 5)
		}

		Tasks.sync {
			entity.world.playSound(entity.location, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 2.0f)
		}
	}
}
