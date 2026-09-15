package net.horizonsend.ion.server.features.starship.subsystem.weapon.secondary

import github.scarsz.discordsrv.dependencies.kyori.adventure.sound.Sound.Source
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.configuration.starship.RailgunBalancing
import net.horizonsend.ion.server.configuration.starship.StarshipSounds
import net.horizonsend.ion.server.core.registration.keys.CustomItemKeys
import net.horizonsend.ion.server.features.client.display.modular.ItemDisplayContainer
import net.horizonsend.ion.server.features.nations.utils.toPlayersInRadius
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.damager.Damager
import net.horizonsend.ion.server.features.starship.destruction.SinkAnimation
import net.horizonsend.ion.server.features.starship.subsystem.weapon.CannonWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.AmmoConsumingWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.HeavyWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.RailgunProjectile
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.source.StarshipProjectileSource
import net.horizonsend.ion.server.features.transport.items.util.EXPLOSION_RING
import net.horizonsend.ion.server.miscellaneous.playDirectionalStarshipSound
import net.horizonsend.ion.server.miscellaneous.playSoundInRadius
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.CopperBulb
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class RailgunWeaponSubsystem(
    starship: ActiveStarship,
    pos: Vec3i,
    face: BlockFace
) : CannonWeaponSubsystem<RailgunBalancing>(starship, pos, face, starship.balancingManager.getWeaponSupplier(RailgunWeaponSubsystem::class)), HeavyWeaponSubsystem, AmmoConsumingWeaponSubsystem {
	override val length: Int = 12
	override val extraDistance: Int = 1

	override val boostChargeNanos: Long get() = balancing.boostChargeNanos

	override fun isAcceptableDirection(face: BlockFace) = true

	override fun fire(loc: Location, dir: Vector, shooter: Damager, target: Vector) {
		toPlayersInRadius(loc, balancing.projectile.range * 20.0) { player ->
			playDirectionalStarshipSound(loc, player, balancing.projectile.fireSoundNear, balancing.projectile.fireSoundNear, balancing.projectile.range)
		}

		Tasks.syncDelay(((20.0 * balancing.warmupTime) - 30.0).toLong()){
			playSoundInRadius(loc,balancing.projectile.range, Sound.sound(key("entity.illusioner.prepare_blindness"), Sound.Source.VOICE, 10f, 0.65f))
			playSoundInRadius(loc,balancing.projectile.range, Sound.sound(key("entity.warden.sonic_charge"), Sound.Source.VOICE, 10f, 1f))
		}

		repeat(3){
			Tasks.syncDelay(((6.67 * balancing.warmupTime * it)).toLong()){
				turnBulbOn(it, getFireVec().toLocation(starship.world))
				return@syncDelay
			}
		}

		Tasks.syncDelay((20.0 * balancing.warmupTime).toLong()) {
			val newFirePos = getFirePos().toCenterVector()
			RailgunProjectile(
				StarshipProjectileSource(starship),
				getName(),
				newFirePos.toLocation(loc.world),
				dir,
				shooter
			).fire()

			RailgunFireShockwaveAnimation(shooter.color).schedule()
			turnAllBulbsOff(getFireVec().toLocation(starship.world))
			playSoundInRadius(loc,balancing.projectile.range, Sound.sound(key("horizonsend:starship.weapon.arsenal_missile.impact"), Sound.Source.VOICE, 10f, 0.01f))
		}
	}

	override fun getName(): Component {
		return Component.text("Railgun")
	}

	override fun isRequiredAmmo(item: ItemStack): Boolean {
		return requireCustomItem(item, CustomItemKeys.STEEL_BLOCK.getValue(), 1)
	}

	override fun consumeAmmo(itemStack: ItemStack) {
		consumeItem(itemStack, 1)
	}

	fun turnBulbOn(bulbIndex: Int, location: Location){
		val newPos = pos.toLocation(location.world).add(face.direction.multiply((3+bulbIndex)))
		val bulb = newPos.block.blockData as CopperBulb
		bulb.isLit = true
		newPos.block.blockData = bulb
	}

	fun turnAllBulbsOff(location: Location){
		for(i in 0..2){
			val newPos = pos.toLocation(location.world).add(face.direction.multiply((3+i)))
			val bulb = newPos.block.blockData as CopperBulb
			bulb.isLit = false
			newPos.block.blockData = bulb
		}
	}

	inner class RailgunFireShockwaveAnimation(color: Color) : BukkitRunnable() {
		private val shockwaveItem = object : SinkAnimation.ColoredSinkAnimationBlock(
			duration = 40,
			wrapper = ItemDisplayContainer(
				world = starship.world,
				initPosition = getFireVec(),
				initHeading = face.direction,
				initScale = 1.0f,
				item = EXPLOSION_RING.construct { t -> t.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color)) },
			),
			direction = Vector(),
			initialScale = 1.0,
			finalScale = 40.0,
			rotationAxis = BlockFace.NORTH.direction,
			rotationDegrees = 0.0,
			colors = mapOf(
				Color.WHITE to 4,
				Color.BLACK to 1
			)
		) {}

		override fun run() {
			shockwaveItem.update()
			if (shockwaveItem.checkDead()) cancel()
		}

		fun schedule() = runTaskTimerAsynchronously(IonServer, 1L, 1L)
	}
}
