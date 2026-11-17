package me.yrsb.evolve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import me.yrsb.evolve.core.engine.EvolveEngine
import me.yrsb.evolve.core.ui.theme.EvolveTheme
import me.yrsb.evolve.feature.evolution.EvolutionScreen

class MainActivity : ComponentActivity() {

    private val engine = EvolveEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start 250ms (4Hz) tick loop bound to activity lifecycleScope
        engine.start(lifecycleScope, intervalMs = 250L)

        setContent {
            EvolveTheme {
                EvolutionScreen(engine = engine)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}
