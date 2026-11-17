package me.yrsb.evolve.core.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.yrsb.evolve.core.model.EvolutionState
import me.yrsb.evolve.core.model.GameCommand
import me.yrsb.evolve.core.model.GameState
import me.yrsb.evolve.core.model.OrganelleState
import me.yrsb.evolve.core.model.ResourcePool
import me.yrsb.evolve.core.model.ResourceType

class EvolveEngine(
    initialState: GameState = GameState()
) {
    private val _gameState = MutableStateFlow(initialState)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Projections
    private val _resourceState = MutableStateFlow(initialState.resources)
    val resourceState: StateFlow<Map<ResourceType, ResourcePool>> = _resourceState.asStateFlow()

    private val _evolutionState = MutableStateFlow(initialState.evolution)
    val evolutionState: StateFlow<EvolutionState> = _evolutionState.asStateFlow()

    private var tickJob: Job? = null

    fun start(scope: CoroutineScope, intervalMs: Long = 250L) {
        if (tickJob != null) return
        tickJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                tick(intervalMs / 1000.0)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    @Synchronized
    fun tick(dt: Double = 0.25) {
        val next = StepFunction.step(_gameState.value, dt)
        _gameState.value = next
        _resourceState.value = next.resources
        _evolutionState.value = next.evolution
    }

    @Synchronized
    fun dispatch(command: GameCommand): Boolean {
        val current = _gameState.value
        return when (command) {
            is GameCommand.ManualSynthesizeRna -> {
                val rnaPool = current.resources[ResourceType.RNA] ?: ResourcePool()
                if (rnaPool.amount < rnaPool.capacity) {
                    val nextAmount = (rnaPool.amount + 1.0).coerceAtMost(rnaPool.capacity)
                    val updatedResources = current.resources.toMutableMap()
                    updatedResources[ResourceType.RNA] = rnaPool.copy(amount = nextAmount)
                    val nextState = current.copy(
                        resources = updatedResources,
                        evolution = current.evolution.copy(manualClicks = current.evolution.manualClicks + 1)
                    )
                    _gameState.value = nextState
                    _resourceState.value = nextState.resources
                    _evolutionState.value = nextState.evolution
                    true
                } else false
            }

            is GameCommand.UpgradeOrganelle -> {
                val type = command.type
                val organelle = current.evolution.organelles[type] ?: OrganelleState()
                val cost = organelle.cost(type)
                val rnaPool = current.resources[ResourceType.RNA] ?: ResourcePool()

                if (rnaPool.amount >= cost) {
                    val updatedRna = rnaPool.copy(amount = rnaPool.amount - cost)
                    val updatedResources = current.resources.toMutableMap()
                    updatedResources[ResourceType.RNA] = updatedRna

                    val updatedOrganelles = current.evolution.organelles.toMutableMap()
                    updatedOrganelles[type] = organelle.copy(count = organelle.count + 1)

                    val nextState = current.copy(
                        resources = updatedResources,
                        evolution = current.evolution.copy(organelles = updatedOrganelles)
                    )
                    _gameState.value = nextState
                    _resourceState.value = nextState.resources
                    _evolutionState.value = nextState.evolution
                    true
                } else false
            }

            is GameCommand.ResetGame -> {
                val fresh = GameState()
                _gameState.value = fresh
                _resourceState.value = fresh.resources
                _evolutionState.value = fresh.evolution
                true
            }
        }
    }
}
