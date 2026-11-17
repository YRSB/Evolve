package me.yrsb.evolve.core.engine

import me.yrsb.evolve.core.model.GameState
import me.yrsb.evolve.core.model.OrganelleType
import me.yrsb.evolve.core.model.ResourcePool
import me.yrsb.evolve.core.model.ResourceType

object StepFunction {
    fun step(state: GameState, dt: Double = 0.25): GameState {
        val organelles = state.evolution.organelles

        // Calculate RNA capacity bonus from Membrane
        val membraneCount = organelles[OrganelleType.MEMBRANE]?.count ?: 0
        val baseRnaCap = 50.0 + (membraneCount * OrganelleType.MEMBRANE.rnaCapBonus)

        // Calculate RNA rate bonus from Cytoplasm and Mitochondria
        val cytoplasmCount = organelles[OrganelleType.CYTOPLASM]?.count ?: 0
        val mitoCount = organelles[OrganelleType.MITOCHONDRIA]?.count ?: 0
        val rnaRate = (cytoplasmCount * OrganelleType.CYTOPLASM.rnaRateBonus) +
                (mitoCount * OrganelleType.MITOCHONDRIA.rnaRateBonus)

        val currentRna = state.resources[ResourceType.RNA] ?: ResourcePool()
        val deltaRna = rnaRate * dt
        val nextRnaAmount = (currentRna.amount + deltaRna).coerceAtMost(baseRnaCap)

        val updatedResources = state.resources.toMutableMap()
        updatedResources[ResourceType.RNA] = currentRna.copy(
            amount = nextRnaAmount,
            capacity = baseRnaCap,
            rate = rnaRate
        )

        return state.copy(
            tick = state.tick + 1,
            resources = updatedResources
        )
    }
}
