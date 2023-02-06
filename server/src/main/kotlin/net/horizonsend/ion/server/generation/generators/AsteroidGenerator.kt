package net.horizonsend.ion.server.generation.generators

import net.horizonsend.ion.server.IonServer.Companion.Ion
import net.horizonsend.ion.server.NamespacedKeys
import net.horizonsend.ion.server.ServerConfiguration
import net.horizonsend.ion.server.generation.Asteroid
import net.horizonsend.ion.server.generation.AsteroidBlockStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.levelgen.Heightmap
import net.starlegacy.util.nms
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.craftbukkit.v1_19_R2.CraftChunk
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.noise.SimplexOctaveGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AsteroidGenerator {
	private const val asteroidGenerationVersion: Byte = 0
	private val oreMap: Map<String, BlockState> = Ion.configuration.ores.associate {
		it.material to Bukkit.createBlockData(it.material).nms
	}
	private val weightedOres = oreWeights()

	fun postGenerateAsteroid(
		serverLevel: ServerLevel,
		asteroid: Asteroid
	) {
		val random = Random(serverLevel.seed)

		// generates a set number of ores per asteroid based on a rough estimate of the block count (volume of a sphere)
		val roughVolume: Double = (
			(3.0 / 4.0) *
				Math.PI *
				((asteroid.size * asteroid.size * asteroid.size) / 3)
			)

		var oresRemaining: Int = (roughVolume * Ion.configuration.oreRatio).roundToInt()
		// Ores generate in veins, but each block decrements the count

		// save some time
		val noise = SimplexOctaveGenerator(random, 1)
		val radiusSquared = asteroid.size * asteroid.size

		// Get every chunk section covered by the asteroid.
		val coveredChunks = mutableMapOf<LevelChunk, Map<LevelChunkSection, Byte>>()

		// generate ranges ahead of time
		val xRange = IntRange(asteroid.x - (asteroid.size * 1.5).toInt(), asteroid.x + (asteroid.size * 1.5).toInt())
		val zRange = IntRange(asteroid.z - (asteroid.size * 1.5).toInt(), asteroid.z + (asteroid.size * 1.5).toInt())
		val yRange = IntRange(
			(asteroid.y - (asteroid.size * 1.5).toInt()).coerceAtLeast(serverLevel.minBuildHeight),
			(asteroid.y + (asteroid.size * 1.5).toInt()).coerceAtMost(serverLevel.maxBuildHeight)
		)

		val chunkXRange = IntRange(xRange.first.shr(4), xRange.last.shr(4))
		val chunkZRange = IntRange(zRange.first.shr(4), zRange.last.shr(4))
		val chunkYRange = IntRange(yRange.first.shr(4), yRange.last.shr(4) + serverLevel.minBuildHeight.shr(4))

		for (chunkPosX in chunkXRange) {
			val xSqr = (chunkPosX - asteroid.x.shr(4)) * (chunkPosX - asteroid.x.shr(4))

			for (chunkPosZ in chunkZRange) {
				val zSqr = (chunkPosZ - asteroid.z.shr(4)) * (chunkPosZ - asteroid.z.shr(4))
				val circle = xSqr + zSqr

				if (circle >= radiusSquared) continue // if out of equatorial radius continue

				val coveredChunk = serverLevel.getChunk(chunkPosX, chunkPosZ)
				val sections = mutableMapOf<LevelChunkSection, Byte>()

				for (chunkSectionY in chunkYRange) {
					val ySqr = (chunkSectionY - asteroid.y.shr(4)) * (chunkSectionY - asteroid.y.shr(4))

					if ((circle + ySqr) <= radiusSquared) {
						sections += coveredChunk.sections[chunkSectionY] to chunkSectionY.toByte()
					}
				}

				coveredChunks[coveredChunk] = sections
			}
		}

		// Covered chunks acquired

		// For each covered chunk
		for ((nmsChunk, sectionList) in coveredChunks) {
			val chunkMinX = nmsChunk.pos.x.shl(4)
			val chunkMinZ = nmsChunk.pos.z.shl(4)

			val formattedSections = mutableListOf<CompoundTag>()

			for ((section, pos) in sectionList) {
				val palette = mutableSetOf<BlockState>()
				val storedBlocks = arrayOfNulls<Int>(4096)
				var index = 0
				val sectionMinY = section.bottomBlockY()

				palette.add(Blocks.AIR.defaultBlockState())

				for (x in 0..15) {
					val worldX = (chunkMinX + x).toDouble()
					val xSquared = (worldX - asteroid.x) * (worldX - asteroid.x)

					for (z in 0..15) {
						val worldZ = (chunkMinZ + z).toDouble()
						val zSquared = (worldZ - asteroid.z) * (worldZ - asteroid.z)

						for (y in 0..15) {
							val worldY = (sectionMinY + y).toDouble()
							val ySquared = (worldY - asteroid.y) * (worldY - asteroid.y)

							var block: BlockState? =
								placeBlock(
									section,
									worldX,
									worldY,
									worldZ,
									xSquared,
									ySquared,
									zSquared,
									x,
									y,
									z,
									asteroid,
									noise
								)

							if (
								(
									random.nextDouble(0.0, roughVolume) <= Ion.configuration.oreRatio &&
										oresRemaining >= 0 &&
										block != null
									) && !block.isAir
							) {
								val ore = weightedOres[random.nextInt(0, OreGenerator.weightedOres.size - 1)]
								block = oreMap[ore.material]
								println("Ore at ${x + worldX} ${y + worldY} ${z + worldZ}")
								oresRemaining--
							}

							if (block != null) {
								palette.add(block)
								storedBlocks[index] = palette.indexOf(block)
							} else {
								storedBlocks[index] = 0
							}

							nmsChunk.playerChunk?.blockChanged(BlockPos(x, y, z))
							index++
						}
					}
				}

				if (storedBlocks.all { it == 0 }) continue // don't write it if it's all empty

				val writtenPalette = palette.map { NbtUtils.writeBlockState(it) }
				val intArray = storedBlocks.requireNoNulls().toIntArray()
				formattedSections += AsteroidBlockStorage.formatSection(pos, intArray, writtenPalette)
			}

			if (sectionList.isEmpty()) continue // everything else is unnecessary

			val storedChunkBlocks = AsteroidBlockStorage.formatChunk(formattedSections, asteroidGenerationVersion)
			val outputStream = ByteArrayOutputStream()
			NbtIo.writeCompressed(storedChunkBlocks, outputStream)
			val byteArray = outputStream.toByteArray()

// 			println(NbtUtils.structureToSnbt(storedChunkBlocks))
			println(byteArray.size)

			Heightmap.primeHeightmaps(nmsChunk, Heightmap.Types.values().toSet())
			nmsChunk.isUnsaved = true
			nmsChunk.playerChunk?.broadcastChanges(nmsChunk)
			nmsChunk.bukkitChunk.persistentDataContainer.set(NamespacedKeys.ASTEROIDS_DATA, PersistentDataType.BYTE_ARRAY, byteArray)
			nmsChunk.persistentDataContainer.set(NamespacedKeys.ASTEROIDS_VERSION, PersistentDataType.BYTE, asteroidGenerationVersion)
		}
	}

	/**
	 * Places the asteroid block, if inside, and returns the block
	 * */
	private fun placeBlock(
		section: LevelChunkSection,
		worldX: Double,
		worldY: Double,
		worldZ: Double,
		worldXSquared: Double,
		worldYSquared: Double,
		worldZSquared: Double,
		localX: Int,
		localY: Int,
		localZ: Int,
		asteroid: Asteroid,
		noise: SimplexOctaveGenerator
	): BlockState? {
		noise.setScale(0.15)

		val weightedMaterials = asteroid.materialWeights()

		// Calculate a noise pattern with a minimum at zero, and a max peak of the size of the materials list.
		val paletteSample = (
			(
				(
					noise.noise(
						worldX,
						worldY,
						worldZ,
						1.0,
						1.0,
						true
					) + 1
					) / 2
				) * (weightedMaterials.size - 1)
			).roundToInt()

		// Weight the list by adding duplicate entries, then sample it for the material.
		val material = weightedMaterials[paletteSample]

		// Full noise is used as the radius of the asteroid, and it is offset by the noise of each block pos.
		var fullNoise = 0.0

		for (octave in 0..asteroid.octaves) {
			noise.setScale(0.015 * (octave + 1.0).pow(2.25))

			val offset = abs(
				noise.noise(worldX, worldY, worldZ, 0.0, 1.0, false)
			) * (asteroid.size / (octave + 1.0).pow(2.25))

			fullNoise += offset
		}

		fullNoise *= fullNoise

		// Continue if block is not inside any asteroid
		if (worldXSquared + worldYSquared + worldZSquared >= fullNoise) return null

		noise.setScale(0.15)

		section.setBlockState(
			localX,
			localY,
			localZ,
			material
		)

		return material
	}

	fun rebuildChunkAsteroids(chunk: Chunk) {
		val storedAsteroidData = chunk.persistentDataContainer.get(NamespacedKeys.ASTEROIDS_DATA, PersistentDataType.BYTE_ARRAY) ?: return
		val nbt = try { NbtIo.readCompressed(ByteArrayInputStream(storedAsteroidData)) } catch (error: Error) {
			Throwable("Could not serialize stored asteroid data!"); error.printStackTrace(); return
		}

		val levelChunk = (chunk as CraftChunk).handle

		val sections = nbt.getList("sections", 9) // listTag

		for (section in sections) {
			val compound = section as CompoundTag
			val levelChunkSection = levelChunk.sections[compound.getByte("y").toInt()]
			val blocks: IntArray = compound.getIntArray("blocks")
			val paletteList = compound.getList("palette", 10)

			val blockStateList = paletteList.toList().map {
				NbtUtils.readBlockState(levelChunk.level.level.holderLookup(Registries.BLOCK), it as CompoundTag)
			}

			var index = 0

			for (x in 0..15) {
				for (z in 0..15) {
					for (y in 0..15) {
						val block = blockStateList[blocks[index]]

						levelChunkSection.setBlockState(x, y, z, block)

						index++
					}
				}
			}
		}
	}

	fun generateRandomAsteroid(x: Int, y: Int, z: Int, random: Random): Asteroid {
		val noise = SimplexOctaveGenerator(random, 1)

		noise.setScale(0.15)

		val weightedPalette = paletteWeights()
		val paletteSample = random.nextInt(weightedPalette.size)

		val blockPalette: ServerConfiguration.Palette = weightedPalette[paletteSample]

		val size = random.nextDouble(5.0, Ion.configuration.maxAsteroidSize)
		val octaves = floor(5 * 0.95.pow(size)).toInt().coerceAtLeast(1)

		return Asteroid(x, y, z, blockPalette, size, octaves)
	}

	fun parseDensity(world: ServerLevel, x: Double, y: Double, z: Double): Double {
		val densities = mutableSetOf<Double>()
		densities.add(Ion.configuration.baseAsteroidDensity)

		for (feature in Ion.configuration.features) {
			if (feature.worldName != world.serverLevelData.levelName) continue

			if ((sqrt((x - feature.x).pow(2) + (z - feature.z).pow(2)) - feature.tubeSize).pow(2) + (y - feature.y).pow(
					2
				) < feature.tubeRadius.pow(2)
			) {
				densities.add(feature.baseDensity)
			}
		}

		return densities.max()
	}

	/**
	 * Weights the list of Palettes in the configuration by adding duplicate entries based on the weight.
	 */
	private fun paletteWeights(): List<ServerConfiguration.Palette> {
		val weightedList = mutableListOf<ServerConfiguration.Palette>()

		for (palette in Ion.configuration.blockPalettes) {
			for (occurrence in palette.weight downTo 0) {
				weightedList.add(palette)
			}
		}

		return weightedList
	}

	private fun oreWeights(): List<ServerConfiguration.Ore> {
		val weightedList = mutableListOf<ServerConfiguration.Ore>()

		for (ore in Ion.configuration.ores) {
			for (occurrence in ore.rolls downTo 0) {
				weightedList.add(ore)
			}
		}

		return weightedList
	}
}
