package me.yrsb.evolve.core.engine

import me.yrsb.evolve.core.model.GameCommand
import me.yrsb.evolve.core.model.GameState
import me.yrsb.evolve.core.model.OrganelleState
import me.yrsb.evolve.core.model.OrganelleType
import me.yrsb.evolve.core.model.ResourcePool
import me.yrsb.evolve.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepFunctionTest {

    @Test
    fun testStepAdvancesTickAndAccumulatesRna() {
        val initial = GameState(
            evolution = me.yrsb.evolve.core.model.EvolutionState(
                organelles = mapOf(
                    OrganelleType.CYTOPLASM to OrganelleState(count = 4) // 4 * 0.25 = 1.0 RNA/sec
                )
            )
        )

        // 1 second step (dt = 1.0)
        val stepped = StepFunction.step(initial, dt = 1.0)
        assertEquals(1L, stepped.tick)
        val rna = stepped.resources[ResourceType.RNA]!!
        assertEquals(1.0, rna.rate, 0.001)
        assertEquals(1.0, rna.amount, 0.001)
    }

    @Test
    fun testEngineManualSynthesizeAndUpgrade() {
        val engine = EvolveEngine()
        // Synthesize 10 RNA
        repeat(10) {
            val success = engine.dispatch(GameCommand.ManualSynthesizeRna)
            assertTrue(success)
        }

        assertEquals(10.0, engine.resourceState.value[ResourceType.RNA]!!.amount, 0.001)
        assertEquals(10L, engine.evolutionState.value.manualClicks)

        // Upgrade Membrane (costs 10 RNA)
        val upgradeSuccess = engine.dispatch(GameCommand.UpgradeOrganelle(OrganelleType.MEMBRANE))
        assertTrue(upgradeSuccess)

        // RNA should be 0, membrane count should be 1
        assertEquals(0.0, engine.resourceState.value[ResourceType.RNA]!!.amount, 0.001)
        assertEquals(1, engine.evolutionState.value.organelles[OrganelleType.MEMBRANE]!!.count)
    }
}
