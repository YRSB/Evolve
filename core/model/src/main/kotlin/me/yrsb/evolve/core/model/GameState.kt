package me.yrsb.evolve.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EvolutionState(
    val organelles: Map<OrganelleType, OrganelleState> = OrganelleType.entries.associateWith { OrganelleState() },
    val manualClicks: Long = 0
)

@Serializable
data class GameState(
    val tick: Long = 0,
    val resources: Map<ResourceType, ResourcePool> = mapOf(
        ResourceType.RNA to ResourcePool(amount = 0.0, capacity = 50.0, rate = 0.0),
        ResourceType.DNA to ResourcePool(amount = 0.0, capacity = 20.0, rate = 0.0)
    ),
    val evolution: EvolutionState = EvolutionState()
)
