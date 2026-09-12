package net.horizonsend.ion.server.features.starship.subsystem.weapon.secondary

import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.server.configuration.starship.PhaserBalancing
import net.horizonsend.ion.server.configuration.starship.RailgunBalancing
import net.horizonsend.ion.server.core.registration.keys.CustomItemKeys
import net.horizonsend.ion.server.features.nations.utils.toPlayersInRadius
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.damager.Damager
import net.horizonsend.ion.server.features.starship.subsystem.weapon.CannonWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.AmmoConsumingWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.HeavyWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.PhaserProjectile
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.RailgunProjectile
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.source.StarshipProjectileSource
import net.horizonsend.ion.server.miscellaneous.playDirectionalStarshipSound
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.FaceAttachable
import org.bukkit.block.data.type.Grindstone
import org.bukkit.block.data.type.Hopper
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

class RailgunWeaponSubsystem(
    starship: ActiveStarship,
    pos: Vec3i,
    face: BlockFace
) : CannonWeaponSubsystem<RailgunBalancing>(starship, pos, face, starship.balancingManager.getWeaponSupplier(RailgunWeaponSubsystem::class)), HeavyWeaponSubsystem, AmmoConsumingWeaponSubsystem {
	override val length: Int = 9

	override val boostChargeNanos: Long get() = balancing.boostChargeNanos

	companion object {
		private const val WARM_UP_TIME_SECONDS = 1.5
	}

	override fun isAcceptableDirection(face: BlockFace) = true

	override fun fire(loc: Location, dir: Vector, shooter: Damager, target: Vector) {

		toPlayersInRadius(loc, balancing.projectile.range * 20.0) { player ->
			playDirectionalStarshipSound(loc, player, balancing.projectile.fireSoundNear, balancing.projectile.fireSoundNear, balancing.projectile.range)
		}

		Tasks.syncDelay((20.0 * WARM_UP_TIME_SECONDS).toLong()) {
			val newFirePos = getFirePos().toCenterVector()
			RailgunProjectile(
				StarshipProjectileSource(starship),
				getName(),
				newFirePos.toLocation(loc.world),
				dir,
				shooter
			).fire()
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
}
