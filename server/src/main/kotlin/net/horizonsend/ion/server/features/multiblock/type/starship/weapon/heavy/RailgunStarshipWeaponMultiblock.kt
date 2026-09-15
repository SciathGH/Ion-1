package net.horizonsend.ion.server.features.multiblock.type.starship.weapon.heavy

import net.horizonsend.ion.server.core.registration.keys.CustomBlockKeys
import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.features.multiblock.type.DisplayNameMultilblock
import net.horizonsend.ion.server.features.multiblock.type.starship.weapon.SignlessStarshipWeaponMultiblock
import net.horizonsend.ion.server.features.multiblock.util.PrepackagedPreset
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.weapon.secondary.RailgunWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.RelativeFace
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.block.BlockFace

object RailgunStarshipWeaponMultiblock : SignlessStarshipWeaponMultiblock<RailgunWeaponSubsystem>(),
	DisplayNameMultilblock {
	override val key: String = "railgun"

	override val displayName: Component get() = text("Railgun")
	override val description: Component get() = text("Powerful spinally mounted heavy weapon. Extreme shield damage.")

	override fun MultiblockShape.buildStructure() {
		z(0) {
			y(0) {
				x(1).customBlock(CustomBlockKeys.ASSEMBLY_CORE.getValue())
				x(0).sponge()
				x(-1).customBlock(CustomBlockKeys.ASSEMBLY_CORE.getValue())
			}
			y(-1) {
				x(0).customBlock(CustomBlockKeys.ASSEMBLY_CORE.getValue())
			}
			y(1) {
				x(0).customBlock(CustomBlockKeys.ASSEMBLY_CORE.getValue())
			}
		}
		z(1) {
			y(0) {
				x(1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.FORWARD))
				x(0).customBlock(CustomBlockKeys.VANADIUM_BLOCK.getValue())
				x(-1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.FORWARD))
			}
			y(-1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.BACKWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.BACKWARD, example = Material.GRINDSTONE.createBlockData()))
			}
		}
		z(2) {
			y(0) {
				x(1).anyCopperVariant()
				x(0).customBlock(CustomBlockKeys.VANADIUM_BLOCK.getValue())
				x(-1).anyCopperVariant()
			}
			y(-1) {
				x(0).anyCopperVariant()
			}
			y(1) {
				x(0).anyCopperVariant()
			}
		}
		z(3) {
			y(0) {
				x(1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.LEFT, RelativeFace.FORWARD))
				x(0).anyCopperBulb()
				x(-1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.FORWARD, RelativeFace.RIGHT))
			}
			y(-1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
		}
		z(4) {
			y(0) {
				x(1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.LEFT, RelativeFace.FORWARD))
				x(0).anyCopperBulb()
				x(-1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.FORWARD, RelativeFace.RIGHT))
			}
			y(-1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
		}
		z(5) {
			y(0) {
				x(1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.LEFT, RelativeFace.FORWARD))
				x(0).anyCopperBulb()
				x(-1).anyGlassPane(PrepackagedPreset.pane(RelativeFace.BACKWARD, RelativeFace.FORWARD, RelativeFace.RIGHT))
			}
			y(-1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
		}
		z(6) {
			y(0) {
				x(1).anyCopperVariant()
				x(0).customBlock(CustomBlockKeys.VANADIUM_BLOCK.getValue())
				x(-1).anyCopperVariant()
			}
			y(-1) {
				x(0).anyCopperVariant()
			}
			y(1) {
				x(0).anyCopperVariant()
			}
		}
		z(7) {
			y(-1) {
				x(0).anyWall()
			}
			y(0) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).anyWall()
			}
		}
		z(8) {
			y(-1) {
				x(0).anyWall()
			}
			y(0) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).anyWall()
			}
		}
		z(9) {
			y(-1) {
				x(0).anyWall()
			}
			y(0) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).anyWall()
			}
		}
		z(10) {
			y(-1) {
				x(0).anyWall()
			}
			y(0) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).anyWall()
			}
		}
		z(11) {
			y(-1) {
				x(0).anyWall()
			}
			y(0) {
				x(0).grindstone(PrepackagedPreset.simpleDirectional(RelativeFace.FORWARD, example = Material.GRINDSTONE.createBlockData()))
			}
			y(1) {
				x(0).anyWall()
			}
		}
		z(12) {
			y(-1) {
				x(0).anyWall()
			}
			y(1) {
				x(0).anyWall()
			}
		}
	}
	override fun createSubsystem(
		starship: ActiveStarship,
		pos: Vec3i,
		face: BlockFace
	): RailgunWeaponSubsystem {
		return RailgunWeaponSubsystem(starship, pos, face)
	}

}
