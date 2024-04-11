package net.horizonsend.ion.server.features.customitems.powerArmour.modules

import net.horizonsend.ion.server.configuration.PVPBalancingConfiguration
import net.horizonsend.ion.server.features.customitems.CustomItem

abstract class PowerArmourModule<T: PVPBalancingConfiguration.Modules.ModulesBalancing>(
	identifier: String

): CustomItem(identifier) {
}
