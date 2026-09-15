package net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile

import net.horizonsend.ion.server.configuration.starship.RailgunBalancing
import net.horizonsend.ion.server.features.multiblock.type.starship.weapon.heavy.RailgunStarshipWeaponMultiblock
import net.horizonsend.ion.server.features.starship.damager.Damager
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.source.ProjectileSource
import net.horizonsend.ion.server.features.starship.subsystem.weapon.projectile.source.StarshipProjectileSource
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.getPointsBetween
import net.kyori.adventure.text.Component
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector

class RailgunProjectile(
	source: ProjectileSource,
	name: Component,
	loc: Location,
	dir: Vector,
	shooter: Damager
) : ParticleProjectile<RailgunBalancing.RailgunProjectileBalancing>(source, name, loc, dir, shooter, RailgunStarshipWeaponMultiblock.damageType) {
	override fun tick(){
		delta = (System.nanoTime() - lastTick) / 1_000_000_000.0 // Convert to seconds

		val predictedNewLoc = location.clone().add(direction.clone().multiply(delta * speed))
		if (!predictedNewLoc.isChunkLoaded) {
			return onDespawn()
		}

		val travel = location.distance(predictedNewLoc)
		val blocksInLine = getPointsBetween(location.toVector(), predictedNewLoc.toVector(), travel.toInt())

		moveVisually(location, predictedNewLoc, travel)

		for (block in blocksInLine) {
			val loc = block.toLocation(location.world)
			val result: RayTraceResult? = location.world.rayTrace(
				loc,
				direction,
				.5,
				FluidCollisionMode.NEVER,
				true,
				0.1,
				{ it !is Display && it.type != EntityType.INTERACTION},
				{
					if (source !is StarshipProjectileSource) true // projectile was not fired from a starship
					else !source.starship.contains(it.x, it.y, it.z) // can collide with any block that is not part of the firing starship
				}
			)
			if (result != null) {
				tryImpact(result, loc)
			}
			if (this.starshipShieldDamageMultiplier <= 0.0){
				this.hasHit = true
				return onDespawn()
			}
		}


		location = predictedNewLoc

		distance += travel

		if (distance >= range || this.starshipShieldDamageMultiplier <= 0.0) {
			this.hasHit = true
			return onDespawn()
		}

		lastTick = System.nanoTime()
		reschedule()
	}

	override fun impact(newLoc: Location, block: Block?, entity: Entity?) {
		super.impact(newLoc, block, entity)
		val br = CraftMagicNumbers.getBlock(block?.type ?: Material.STONE_BRICKS).explosionResistance
		this.starshipShieldDamageMultiplier -= br
		hasHit = false;
	}

	override fun spawnParticle(x: Double, y: Double, z: Double, force: Boolean) {
		val offset = 0.0
		val count = 1
		val extra = 0.0
		val data = null
		location.world.spawnParticle(Particle.GUST, x, y, z, count, offset, offset, offset, extra, data, force)
	}
}
