package me.yrsb.evolve.feature.evolution

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.yrsb.evolve.core.engine.EvolveEngine
import me.yrsb.evolve.core.ui.theme.EvolveTheme
import me.yrsb.evolve.feature.evolution.prototype.PrototypeSwitcher
import me.yrsb.evolve.feature.evolution.prototype.PrototypeVariant
import me.yrsb.evolve.feature.evolution.prototype.VariantA_PillBottomSheet
import me.yrsb.evolve.feature.evolution.prototype.VariantB_GridDropdown
import me.yrsb.evolve.feature.evolution.prototype.VariantC_CollapsibleInline

@Composable
fun EvolutionScreen(
    engine: EvolveEngine,
    modifier: Modifier = Modifier
) {
    val resources by engine.resourceState.collectAsStateWithLifecycle()
    val evolution by engine.evolutionState.collectAsStateWithLifecycle()
    val gameState by engine.gameState.collectAsStateWithLifecycle()

    var currentVariant by remember { mutableStateOf(PrototypeVariant.VARIANT_A) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentVariant) {
                PrototypeVariant.VARIANT_A -> {
                    VariantA_PillBottomSheet(
                        engine = engine,
                        gameState = gameState,
                        resources = resources,
                        evolution = evolution
                    )
                }
                PrototypeVariant.VARIANT_B -> {
                    VariantB_GridDropdown(
                        engine = engine,
                        gameState = gameState,
                        resources = resources,
                        evolution = evolution
                    )
                }
                PrototypeVariant.VARIANT_C -> {
                    VariantC_CollapsibleInline(
                        engine = engine,
                        gameState = gameState,
                        resources = resources,
                        evolution = evolution
                    )
                }
            }

            // 浮动原型切换器：固定于底部中央，方便快速横向体验比对
            PrototypeSwitcher(
                current = currentVariant,
                onSelectVariant = { currentVariant = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview(name = "Evolution Screen Full Flow", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun EvolutionScreenPreview() {
    val engine = remember { EvolveEngine() }
    EvolveTheme {
        EvolutionScreen(engine = engine)
    }
}
