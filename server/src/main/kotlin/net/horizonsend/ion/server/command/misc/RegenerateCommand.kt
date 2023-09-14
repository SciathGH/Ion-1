package net.horizonsend.ion.server.command.misc

import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.mojang.serialization.DataResult
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import net.horizonsend.ion.common.extensions.information
import net.horizonsend.ion.common.extensions.serverError
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.command.SLCommand
import net.horizonsend.ion.server.features.space.data.BlockData
import net.horizonsend.ion.server.features.space.data.CompletedSection
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.minecraft
import net.kyori.adventure.audience.Audience
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.storage.ChunkSerializer
import net.minecraft.world.level.chunk.storage.RegionFile
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File

@CommandPermission("ion.regenerate")
@CommandAlias("regenerate")
object RegenerateCommand : SLCommand() {
	// The worlds in this folder are stripped down versions of worlds. Basically just renamed region folders
	private val cleanWorldsFolder: File = IonServer.dataFolder.resolve("worlds")
	private val scope = CoroutineScope(Dispatchers.Default)
	private val blockStateCodec = ChunkSerializer.BLOCK_STATE_CODEC

	@Default
	@Suppress("unused")
	fun onRegenerate(sender: Player) {
		val session = WorldEdit.getInstance().sessionManager.findByName(sender.name) ?: return
		val selection = session.getSelection(session.selectionWorld)

		sender.information("Started regenerating ${sender.world.name} from ${selection.minimumPoint} to ${selection.maximumPoint}")
		val time = System.currentTimeMillis()

		val sections = mutableMapOf<Triple<Int, Int, Int>, CompletableDeferred<Pair<ChunkPos, CompletedSection>>>()
		val sectionsHeight = IntRange(selection.minimumPoint.y.shr(4), selection.maximumPoint.y.shr(4))

		// Group by string first to avoid getting the region dozens of times
		val regionsToChunksMap = selection.chunks.groupBy {
			val regionX = it.x.shr(5)
			val regionZ = it.z.shr(5)

			"r.$regionX.$regionZ.mca"
		}

		for (chunk in selection.chunkCubes) {
			sections[Triple(chunk.x, chunk.y, chunk.z)] = CompletableDeferred()
		}

		for ((regionFile, chunks) in regionsToChunksMap) {
			scope.launch {
				val region = getRegion(sender.world, regionFile) ?: return@launch sender.serverError(
					"Region file ${chunks.first().x.shr(5)}, ${chunks.first().z.shr(5)} doesn't exist!"
				)

				for (chunk in chunks) scope.launch chunk@{
					fun removeDeferredChunkSections() {
						val chunkSections = sections.filterKeys { it.first == chunk.x && it.third == chunk.z }

						for ((location, _) in chunkSections) {
							sections.remove(location)
						}
					}

					val chunkPos = ChunkPos(chunk.x, chunk.z)

					if (!region.doesChunkExist(chunkPos)) {
						removeDeferredChunkSections()
						sender.serverError("Chunk [${chunk.x}, ${chunk.z}] was not in Region file ${chunks.first().x.shr(5)}! Skipping.")
						return@chunk
					}

					val chunkData = region.getChunkDataInputStream(chunkPos)?.let { NbtIo.read(it) }

					if (chunkData == null) {
						sender.serverError("Chunk [${chunk.x}, ${chunk.z}] could not be read from Region file ${chunks.first().x.shr(5)}! Skipping.")
						removeDeferredChunkSections()
						return@chunk
					}

					@Suppress("UNCHECKED_CAST")
					val sectionsList = (chunkData.getList("sections", 10).toList() as List<CompoundTag>)
						.associateBy { it.getByte("Y") }

					for (sectionY in sectionsHeight) {
						val sectionPos = Triple(chunk.x, sectionY, chunk.z)
						val storedSection = sectionsList[sectionY.toByte()]

						if (storedSection == null) {
							sender.serverError("Stored section for $sectionPos was not found. Skipping.")
							sections.remove(sectionPos)
							continue
						}

						val deferred = sections[sectionPos]!! // I hope not

						val dataResult = blockStateCodec.parse(NbtOps.INSTANCE, storedSection.getCompound("block_states"))

						val sectionBlocks = (dataResult as DataResult<PalettedContainer<BlockState?>>).getOrThrow(false) {
							sender.serverError("Error reading section blocks: $it")
							IonServer.slF4JLogger.warn(it)
						}

						regenerateSection(sender, sectionY, chunkPos, sectionBlocks, deferred, selection)
					}
				}
			}
		}

		IonServer.logger.info(
			"""
				Awaiting: ${sections.map { "${it.key}: ${it.value}" }}
			""".trimIndent()
		)

		scope.launch { complete(sender.world, sections.values) }.invokeOnCompletion {
			val diff = System.currentTimeMillis() - time

			sender.information("Took $diff ms")
		}
	}

	private suspend fun complete(world: World, deferredSections: Collection<CompletableDeferred<Pair<ChunkPos, CompletedSection>>>) {
		val newSections = deferredSections.toMutableSet()

		val sections = newSections.awaitAll()

		val chunkMap = sections.groupBy { it.first }.mapKeys { world.minecraft.getChunk(it.key.x, it.key.z) }

		for ((levelChunk, groupedSections) in chunkMap) {
			Tasks.sync {
				for ((_, section) in groupedSections) {

					section.place(levelChunk)
				}
			}
		}
	}

	private fun regenerateSection(
		audience: Audience,
		sectionY: Int,
		chunkPos: ChunkPos,
		palettedContainer: PalettedContainer<BlockState?>,
		deferred: CompletableDeferred<Pair<ChunkPos, CompletedSection>>,
		selection: Region
	) {
		val newSection = CompletedSection.empty(sectionY)

		for (x in 0..15) for (y in 0..15) for (z in 0..15) {
			val realX = x + (chunkPos.x.shl(4))
			val realY = y + (sectionY.shl(4))
			val realZ = z + (chunkPos.z.shl(4))

			if (!selection.contains(BlockVector3.at(realX, realY, realZ))) {
				continue
			}

			val index = PalettedContainer.Strategy.SECTION_STATES.getIndex(x, y, z)

			val state: BlockState? = palettedContainer[index]

			if (state == null) {
				audience.serverError("Block at $realX, $realY, $realZ was null in the container!")
				continue
			}

			newSection.setBlock(index, BlockData(state, null))
		}

		IonServer.logger.info("Completed section ${chunkPos.x}, $sectionY, ${chunkPos.z}")
		deferred.complete(chunkPos to newSection)
	}

	private fun getRegion(world: World, regionFileName: String): RegionFile? {
		val region = cleanWorldsFolder.resolve(world.name)

		if (!region.exists()) return null

		try {
			return RegionFile(region.resolve(regionFileName).toPath(), region.toPath(), false)
		} catch (error: Error) {
			throw error
		}
	}
}
