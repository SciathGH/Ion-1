package net.horizonsend.ion.server.features.nodes

import org.bukkit.Location

abstract class AbstractNode {
	abstract val location: Location
	abstract val endNodes: MutableList<AbstractNode>
	open fun output(){}
}
