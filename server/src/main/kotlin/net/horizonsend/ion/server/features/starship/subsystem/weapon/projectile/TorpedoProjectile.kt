package net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile

import net.horizonsend.ion.common.utils.miscellaneous.d
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.command.admin.GracePeriod
import net.horizonsend.ion.server.command.admin.debug
import net.horizonsend.ion.server.configuration.StarshipWeapons
import net.horizonsend.ion.server.features.machine.AreaShields
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.active.ActiveStarships
import net.horizonsend.ion.server.features.starship.damager.Damager
import net.horizonsend.ion.server.features.starship.damager.EntityDamager
import net.horizonsend.ion.server.features.starship.damager.PlayerDamager
import net.horizonsend.ion.server.features.starship.subsystem.shield.StarshipShields
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector

class TorpedoProjectile(
    starship: ActiveStarship?,
    loc: Location,
    dir: Vector,
    shooter: Damager,
    originalTarget: Vector,
    baseAimDistance: Int
) : TrackingLaserProjectile(starship, loc, dir, shooter, originalTarget, baseAimDistance) {
	override val balancing: StarshipWeapons.ProjectileBalancing = starship?.balancing?.weapons?.protonTorpedo ?: IonServer.starshipBalancing.nonStarshipFired.protonTorpedo

	override val range: Double = balancing.range
	override val speed: Double = balancing.speed
	override val starshipShieldDamageMultiplier = balancing.starshipShieldDamageMultiplier
	override val areaShieldDamageMultiplier: Double = balancing.areaShieldDamageMultiplier
	override val color: Color = Color.fromRGB(255, 0, 255)
	override val particleThickness: Double = balancing.particleThickness
	override val explosionPower: Float = balancing.explosionPower
	override val maxDegrees: Double = balancing.maxDegrees
	override val volume: Int = balancing.volume
	override val pitch: Float = balancing.pitch
	override val soundName: String = balancing.soundName
	override fun impact(newLoc: Location, block: Block?, entity: Entity?) {
		if (GracePeriod.isGracePeriod) return

		val world = newLoc.world
		if (world.environment == World.Environment.NETHER && world.name.contains("hyperspace", ignoreCase=true)) {
			return
		}

		// use these so we dont use hardcoded Material values
		val armorBlastResist = CraftMagicNumbers.getBlock(Material.STONE).explosionResistance
		val impactedBlastResist = CraftMagicNumbers.getBlock(block?.type ?: Material.STONE_BRICKS).explosionResistance
		val fraction = 1.0 + (armorBlastResist - impactedBlastResist) / 20.0

		starship?.debug(
			"ship dmg: \n\n" +
				"armorBlastResist = $armorBlastResist, \n" +
				"impactedBlastResist = $impactedBlastResist, \n" +
				"fraction = $fraction, \n" +
				"shieldDamageMultiplier = $starshipShieldDamageMultiplier, \n" +
				"result = ${fraction * explosionPower * starshipShieldDamageMultiplier}"
		)
		val shipSize = ActiveStarships.findByBlock(newLoc)?.initialBlockCount ?: 700.0
		val starshipSizeModifier = (500.0/shipSize.d()).coerceAtLeast(1.0)

		StarshipShields.withExplosionPowerOverride(fraction * explosionPower * starshipShieldDamageMultiplier*starshipSizeModifier) {
			AreaShields.withExplosionPowerOverride(fraction * explosionPower * starshipShieldDamageMultiplier*starshipSizeModifier) {
				if (!hasHit) {
					world.createExplosion(newLoc, explosionPower)

					world.spawnParticle(
						Particle.FLASH,
						newLoc.x,
						newLoc.y,
						newLoc.z,
						explosionPower.toInt(),
						explosionPower.toDouble() / 2,
						explosionPower.toDouble() / 2,
						explosionPower.toDouble() / 2,
						0.0,
						null,
						true
					)
					hasHit = true
				}
			}
		}

		if (block != null) addToDamagers(world, block, shooter)

		if (entity != null && entity is LivingEntity) when (shooter) {
			is PlayerDamager -> entity.damage(10.0, shooter.player)
			is EntityDamager -> entity.damage(10.0, shooter.entity)
			else -> entity.damage(10.0)
		}
	}
}
