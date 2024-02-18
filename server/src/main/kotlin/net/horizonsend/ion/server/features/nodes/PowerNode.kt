package net.horizonsend.ion.server.features.nodes

abstract class PowerNode : AbstractNode() {
	var power: Double = 0.0
	open fun input(powerToAdd: Double){
		power+=powerToAdd
	}
	open fun output(nodeToOutput: PowerNode, outputPower: Double){
		power-=outputPower
		nodeToOutput.input(outputPower)
	}
}
