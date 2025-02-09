package net.horizonsend.ion.server.features.explosions.utilities

import io.papermc.paper.entity.LookAnchor
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.alongVector
import net.horizonsend.ion.server.miscellaneous.utils.toVector3f
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.Closeable
import kotlin.math.cos
import kotlin.math.sin

class CameraShakeOptions (
    var magnitude: Double,
    var decay: Double,
    var pitchPeriod: Double,
    var yawPeriod: Double,
)

class CameraShake(
	player: Player,
	options: CameraShakeOptions,
): Closeable {
    private var time = 0
    private var prevPitch = .0
    private var prevYaw = .0

    private var magnitude = options.magnitude

    private val repeat = Tasks.asyncRepeat(1,1) {
        time += 1

        magnitude -= options.decay

        if (magnitude < 0) {
            close()
            return@asyncRepeat
        }

        val pitch = sin(time.toDouble() / options.pitchPeriod * 2 * Math.PI) * magnitude
        val yaw = cos(time.toDouble() / options.yawPeriod * 2 * Math.PI) * magnitude

        prevPitch = pitch
        prevYaw = yaw

		val locationInFront = player.eyeLocation.addRotation(yaw.toFloat(), pitch.toFloat()).direction
		val x = locationInFront.x + player.eyeLocation.x
		val y = locationInFront.y + player.eyeLocation.y
		val z = locationInFront.z + player.eyeLocation.z
		player.lookAt(x, y, z, LookAnchor.EYES)
    }

    override fun close() {
		repeat.cancel()
    }
}
